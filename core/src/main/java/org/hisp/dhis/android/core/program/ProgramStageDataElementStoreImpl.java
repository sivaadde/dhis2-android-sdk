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

package org.hisp.dhis.android.core.program;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.Date;

import static org.hisp.dhis.android.core.utils.StoreUtils.sqLiteBind;

@SuppressWarnings({
        "PMD.AvoidDuplicateLiterals"
})
public class ProgramStageDataElementStoreImpl implements ProgramStageDataElementStore {

    private static final String INSERT_STATEMENT = "INSERT INTO " +
            ProgramStageDataElementModel.TABLE + " (" +
            ProgramStageDataElementModel.Columns.UID + ", " +
            ProgramStageDataElementModel.Columns.CODE + ", " +
            ProgramStageDataElementModel.Columns.NAME + ", " +
            ProgramStageDataElementModel.Columns.DISPLAY_NAME + ", " +
            ProgramStageDataElementModel.Columns.CREATED + ", " +
            ProgramStageDataElementModel.Columns.LAST_UPDATED + ", " +
            ProgramStageDataElementModel.Columns.DISPLAY_IN_REPORTS + ", " +
            ProgramStageDataElementModel.Columns.COMPULSORY + ", " +
            ProgramStageDataElementModel.Columns.ALLOW_PROVIDED_ELSEWHERE + ", " +
            ProgramStageDataElementModel.Columns.SORT_ORDER + ", " +
            ProgramStageDataElementModel.Columns.ALLOW_FUTURE_DATE + ", " +
            ProgramStageDataElementModel.Columns.DATA_ELEMENT + ", " +
            ProgramStageDataElementModel.Columns.PROGRAM_STAGE+ ", " +
            ProgramStageDataElementModel.Columns.PROGRAM_STAGE_SECTION + ") " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";

    private static final String UPDATE_WITH_SECTION_STATEMENT = "UPDATE " +
            ProgramStageDataElementModel.TABLE + " SET " +
            ProgramStageDataElementModel.Columns.UID + " =?, " +
            ProgramStageDataElementModel.Columns.CODE + " =?, " +
            ProgramStageDataElementModel.Columns.NAME + " =?, " +
            ProgramStageDataElementModel.Columns.DISPLAY_NAME + " =?, " +
            ProgramStageDataElementModel.Columns.CREATED + " =?, " +
            ProgramStageDataElementModel.Columns.LAST_UPDATED + " =?, " +
            ProgramStageDataElementModel.Columns.DISPLAY_IN_REPORTS + " =?, " +
            ProgramStageDataElementModel.Columns.COMPULSORY + " =?, " +
            ProgramStageDataElementModel.Columns.ALLOW_PROVIDED_ELSEWHERE + " =?, " +
            ProgramStageDataElementModel.Columns.SORT_ORDER + " =?, " +
            ProgramStageDataElementModel.Columns.ALLOW_FUTURE_DATE + " =?, " +
            ProgramStageDataElementModel.Columns.DATA_ELEMENT + " =?, " +
            ProgramStageDataElementModel.Columns.PROGRAM_STAGE + " =?, " +
            ProgramStageDataElementModel.Columns.PROGRAM_STAGE_SECTION + " =?" +
            " WHERE " +
            ProgramStageDataElementModel.Columns.UID + " =?;";

    private static final String UPDATE_WITHOUT_SECTION_STATEMENT = "UPDATE " +
            ProgramStageDataElementModel.TABLE + " SET " +
            ProgramStageDataElementModel.Columns.UID + " =?, " +
            ProgramStageDataElementModel.Columns.CODE + " =?, " +
            ProgramStageDataElementModel.Columns.NAME + " =?, " +
            ProgramStageDataElementModel.Columns.DISPLAY_NAME + " =?, " +
            ProgramStageDataElementModel.Columns.CREATED + " =?, " +
            ProgramStageDataElementModel.Columns.LAST_UPDATED + " =?, " +
            ProgramStageDataElementModel.Columns.DISPLAY_IN_REPORTS + " =?, " +
            ProgramStageDataElementModel.Columns.COMPULSORY + " =?, " +
            ProgramStageDataElementModel.Columns.ALLOW_PROVIDED_ELSEWHERE + " =?, " +
            ProgramStageDataElementModel.Columns.SORT_ORDER + " =?, " +
            ProgramStageDataElementModel.Columns.ALLOW_FUTURE_DATE + " =?, " +
            ProgramStageDataElementModel.Columns.DATA_ELEMENT + " =?, " +
            ProgramStageDataElementModel.Columns.PROGRAM_STAGE + " =?" +
            " WHERE " +
            ProgramStageDataElementModel.Columns.UID + " =?;";

    private static final String DELETE_STATEMENT = "DELETE FROM " + ProgramStageDataElementModel.TABLE + " WHERE " +
            ProgramStageDataElementModel.Columns.UID + " =?;";

    private final SQLiteStatement insertStatement;
    private final SQLiteStatement updateWithSectionStatement;
    private final SQLiteStatement updateWithoutSectionStatement;
    private final SQLiteStatement deleteStatement;

    public ProgramStageDataElementStoreImpl(SQLiteDatabase sqLiteDatabase) {
        this.insertStatement = sqLiteDatabase.compileStatement(INSERT_STATEMENT);
        this.updateWithSectionStatement = sqLiteDatabase.compileStatement(UPDATE_WITH_SECTION_STATEMENT);
        this.updateWithoutSectionStatement = sqLiteDatabase.compileStatement(UPDATE_WITHOUT_SECTION_STATEMENT);
        this.deleteStatement = sqLiteDatabase.compileStatement(DELETE_STATEMENT);
    }

    @Override
    public long insert(@NonNull String uid, @Nullable String code, @Nullable String name,
                       @Nullable String displayName, @NonNull Date created,
                       @NonNull Date lastUpdated, @NonNull Boolean displayInReports,
                       @NonNull Boolean compulsory, @NonNull Boolean allowProvidedElsewhere,
                       @Nullable Integer sortOrder, @NonNull Boolean allowFutureDate,
                       @NonNull String dataElement, @Nullable String programStageUid,
                       @Nullable String programStageSection) {
        bindArguments(insertStatement, uid, code, name, displayName, created, lastUpdated, displayInReports, compulsory,
                allowProvidedElsewhere, sortOrder, allowFutureDate, dataElement, programStageUid);

        // bind the optional argument program stage section
        sqLiteBind(insertStatement, 14, programStageSection);

        Long insert = insertStatement.executeInsert();
        insertStatement.clearBindings();

        return insert;
    }

    // TODO: Refactor updateWithSection and updateWithoutSection when programStageSection is available as a direct
    // TODO: property of the program stage data element. It should be there in March according to Morten and Lars.
    @Override
    public int updateWithSection(@NonNull String uid, @Nullable String code, @Nullable String name,
                                 @Nullable String displayName, @NonNull Date created, @NonNull Date lastUpdated,
                                 @NonNull Boolean displayInReports, @NonNull Boolean compulsory,
                                 @NonNull Boolean allowProvidedElsewhere, @Nullable Integer sortOrder,
                                 @NonNull Boolean allowFutureDate, @NonNull String dataElement,
                                 @Nullable String programStageUid, @Nullable String programStageSection,
                                 @NonNull String whereProgramStageDataElementUid) {
        bindArguments(updateWithSectionStatement, uid, code, name, displayName, created, lastUpdated,
                displayInReports, compulsory, allowProvidedElsewhere, sortOrder, allowFutureDate, dataElement,
                programStageUid);

        // bind the optional argument program stage section
        sqLiteBind(updateWithSectionStatement, 14, programStageSection);

        // bind the where argument
        sqLiteBind(updateWithSectionStatement, 15, whereProgramStageDataElementUid);

        // execute and clear bindings
        int update = updateWithSectionStatement.executeUpdateDelete();
        updateWithSectionStatement.clearBindings();

        return update;
    }


    @Override
    public int updateWithoutSection(@NonNull String uid, @Nullable String code, @Nullable String name,
                                    @Nullable String displayName, @NonNull Date created, @NonNull Date lastUpdated,
                                    @NonNull Boolean displayInReports, @NonNull Boolean compulsory,
                                    @NonNull Boolean allowProvidedElsewhere, @Nullable Integer sortOrder,
                                    @NonNull Boolean allowFutureDate, @NonNull String dataElement,
                                    @Nullable String programStageUid, @NonNull String whereProgramStageDataElementUid) {
        bindArguments(updateWithoutSectionStatement, uid, code, name, displayName, created, lastUpdated,
                displayInReports, compulsory, allowProvidedElsewhere, sortOrder,
                allowFutureDate, dataElement, programStageUid);

        // bind the where argument
        sqLiteBind(updateWithoutSectionStatement, 14, whereProgramStageDataElementUid);

        // execute and clear bindings
        int update = updateWithoutSectionStatement.executeUpdateDelete();
        updateWithoutSectionStatement.clearBindings();

        return update;
    }

    @Override
    public int delete(@NonNull String uid) {
        // bind the where argument
        sqLiteBind(deleteStatement, 1, uid);

        // execute and clear bindings
        int delete = deleteStatement.executeUpdateDelete();
        deleteStatement.clearBindings();

        return delete;
    }

    private void bindArguments(@NonNull SQLiteStatement sqLiteStatement, @NonNull String uid,
                               @Nullable String code, @Nullable String name,
                               @Nullable String displayName, @NonNull Date created,
                               @NonNull Date lastUpdated, @NonNull Boolean displayInReports,
                               @NonNull Boolean compulsory, @NonNull Boolean allowProvidedElsewhere,
                               @Nullable Integer sortOrder, @NonNull Boolean allowFutureDate,
                               @NonNull String dataElement, @Nullable String programStageUid) {
        sqLiteBind(sqLiteStatement, 1, uid);
        sqLiteBind(sqLiteStatement, 2, code);
        sqLiteBind(sqLiteStatement, 3, name);
        sqLiteBind(sqLiteStatement, 4, displayName);
        sqLiteBind(sqLiteStatement, 5, created);
        sqLiteBind(sqLiteStatement, 6, lastUpdated);
        sqLiteBind(sqLiteStatement, 7, displayInReports);
        sqLiteBind(sqLiteStatement, 8, compulsory);
        sqLiteBind(sqLiteStatement, 9, allowProvidedElsewhere);
        sqLiteBind(sqLiteStatement, 10, sortOrder);
        sqLiteBind(sqLiteStatement, 11, allowFutureDate);
        sqLiteBind(sqLiteStatement, 12, dataElement);
        sqLiteBind(sqLiteStatement, 13, programStageUid);
    }

}
