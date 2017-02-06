/*
 * Copyright (c) 2017, University of Oslo
 *
 * All rights reserved.
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.hisp.dhis.android.core.organisationunit;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;

import org.hisp.dhis.android.core.common.Call;
import org.hisp.dhis.android.core.common.Payload;
import org.hisp.dhis.android.core.data.api.Filter;
import org.hisp.dhis.android.core.resource.ResourceModel;
import org.hisp.dhis.android.core.resource.ResourceStore;
import org.hisp.dhis.android.core.user.User;
import org.hisp.dhis.android.core.user.UserOrganisationUnitLinkStore;
import org.hisp.dhis.android.core.utils.HeaderUtils;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import retrofit2.Response;

public class OrganisationUnitCall implements Call<Response<Payload<OrganisationUnit>>> {

    private final User user;
    private final OrganisationUnitService organisationUnitService;
    private final SQLiteDatabase database;
    private final OrganisationUnitStore organisationUnitStore;
    private final UserOrganisationUnitLinkStore userOrganisationUnitLinkStore;
    private final ResourceStore resourceStore;
    private boolean isExecuted;

    public OrganisationUnitCall(User user,
                                OrganisationUnitService organisationUnitService,
                                SQLiteDatabase sqLiteDatabase,
                                OrganisationUnitStore organisationUnitStore,
                                UserOrganisationUnitLinkStore userOrganisationUnitLinkStore,
                                ResourceStore resourceStore
    ) {
        this.user = user;
        this.organisationUnitService = organisationUnitService;
        this.database = sqLiteDatabase;
        this.organisationUnitStore = organisationUnitStore;
        this.userOrganisationUnitLinkStore = userOrganisationUnitLinkStore;
        this.resourceStore = resourceStore;
    }

    @Override
    public boolean isExecuted() {
        synchronized (this) {
            return isExecuted;
        }
    }

    @Override
    public Response<Payload<OrganisationUnit>> call() throws Exception {

        synchronized (this) {
            if (isExecuted) {
                throw new IllegalStateException("AlreadyExecuted");
            }
            isExecuted = true;
        }
        Response<Payload<OrganisationUnit>> response = null;

        database.beginTransaction();
        try {
            Set<String> rootOrgUnitUids = OrganisationUnitTree.findRoots(user.organisationUnits());
            // Call OrganisationUnitService for each tree root & try to persist sub-tree:

            for (String uid : rootOrgUnitUids) {
                response = getOrganisationUnit(uid);
                if (response.isSuccessful()) {
                    persistOrganisationUnits(response.body().items(), response.headers().getDate(HeaderUtils.DATE),
                            OrganisationUnitModel.Scope.SCOPE_DATA_CAPTURE);
                } else {
                    break; //stop early unsuccessful:
                }
            }
            if (response != null && response.isSuccessful()) {
                database.setTransactionSuccessful();
            }
        } finally {
            database.endTransaction();
        }
        return response;
    }

    private Response<Payload<OrganisationUnit>> getOrganisationUnit(@NonNull String uid) throws IOException {
        Map<String, String> queryMap = new HashMap<>();
        String updatedDate = getLastUpdated();
        if (updatedDate != null && !updatedDate.isEmpty()) {
            queryMap.put(OrganisationUnitModel.Columns.LAST_UPDATED,
                    OrganisationUnitModel.Columns.LAST_UPDATED + ":gt:" + updatedDate);
        }

        Filter<OrganisationUnit> filter = Filter.<OrganisationUnit>builder().fields(
                OrganisationUnit.uid, OrganisationUnit.code, OrganisationUnit.name,
                OrganisationUnit.displayName, OrganisationUnit.created,
                //TODO: lookup the date from resource in database and use that:
//                OrganisationUnit.lastUpdated,
                OrganisationUnit.shortName, OrganisationUnit.displayShortName,
                OrganisationUnit.description, OrganisationUnit.displayDescription,
                OrganisationUnit.displayDescription, OrganisationUnit.path, OrganisationUnit.openingDate,
                OrganisationUnit.closedDate, OrganisationUnit.level, OrganisationUnit.deleted,
                OrganisationUnit.parent,
                //TODO: find out if programs are relevant: can they be updated on their own ?
                OrganisationUnit.programs
        ).build();
        retrofit2.Call<Payload<OrganisationUnit>> call = organisationUnitService.getOrganisationUnits(uid, filter,
                queryMap, true, false);
        return call.execute();
    }

    private void persistOrganisationUnits(@NonNull List<OrganisationUnit> organisationUnits, Date serverDate,
                                          OrganisationUnitModel.Scope organisationUnitScope) {

        for (int i = 0, size = organisationUnits.size(); i < size; i++) {
            OrganisationUnit organisationUnit = organisationUnits.get(i);
            if (organisationUnit.deleted()) {
                organisationUnitStore.delete(organisationUnit.uid());
            } else {
                insertOrUpdate(organisationUnit, organisationUnitScope);
            }
            if (i == size - 1) { //if last one:
                updateInResourceStore(serverDate);
            }
        }
    }

    private void insertOrUpdate(@NonNull OrganisationUnit organisationUnit,
                                OrganisationUnitModel.Scope organisationUnitScope) {

        String parentUid = null;
        if (organisationUnit.parent() != null) {
            parentUid = organisationUnit.parent().uid();
        }

        int updatedRow = organisationUnitStore.update(
                organisationUnit.uid(),
                organisationUnit.code(),
                organisationUnit.name(),
                organisationUnit.displayName(),
                organisationUnit.created(),
                organisationUnit.lastUpdated(),
                organisationUnit.shortName(),
                organisationUnit.displayShortName(),
                organisationUnit.description(),
                organisationUnit.displayDescription(),
                organisationUnit.path(),
                organisationUnit.openingDate(),
                organisationUnit.closedDate(),
                parentUid,
                organisationUnit.level(),
                organisationUnit.uid()
        );

        if (updatedRow <= 0) {
            organisationUnitStore.insert(
                    organisationUnit.uid(),
                    organisationUnit.code(),
                    organisationUnit.name(),
                    organisationUnit.displayName(),
                    organisationUnit.created(),
                    organisationUnit.lastUpdated(),
                    organisationUnit.shortName(),
                    organisationUnit.displayShortName(),
                    organisationUnit.description(),
                    organisationUnit.displayDescription(),
                    organisationUnit.path(),
                    organisationUnit.openingDate(),
                    organisationUnit.closedDate(),
                    parentUid,
                    organisationUnit.level()
            );
        }

        // maintain link between user and organisation unit
        int updatedUserOrganisationUnitLinkRow = userOrganisationUnitLinkStore.update(
                user.uid(), organisationUnit.uid(),
                organisationUnitScope.name(),
                user.uid(), organisationUnit.uid()
        );

        if (updatedUserOrganisationUnitLinkRow <= 0) {
            userOrganisationUnitLinkStore.insert(
                    user.uid(), organisationUnit.uid(),
                    organisationUnitScope.name()
            );
        }
    }

    private void updateInResourceStore(Date serverDate) {
        int rowId = resourceStore.update(OrganisationUnit.class.getSimpleName(), serverDate,
                OrganisationUnit.class.getSimpleName());
        if (rowId <= 0) {
            resourceStore.insert(OrganisationUnit.class.getSimpleName(), serverDate);
        }
    }

    private String getLastUpdated() {
        String lastUpdated;
        Cursor cursor = database.query(
                ResourceModel.TABLE,
                new String[]{ResourceModel.Columns.LAST_SYNCED},
                ResourceModel.Columns.RESOURCE_TYPE + "=?",
                new String[]{OrganisationUnit.class.getSimpleName()},
                null, null, null
        );
        cursor.moveToFirst();
        lastUpdated = cursor.getString(cursor.getColumnIndex(ResourceModel.Columns.LAST_SYNCED));
        cursor.close();
        return lastUpdated;
    }
}