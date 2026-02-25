/*
 * Copyright (c) 2023 European Commission
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European
 * Commission - subsequent versions of the EUPL (the "Licence"); You may not use this work
 * except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the Licence is distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific language
 * governing permissions and limitations under the Licence.
 */

package com.k689.identid.storage.service

import androidx.room.Database
import androidx.room.RoomDatabase
import com.k689.identid.model.storage.Bookmark
import com.k689.identid.model.storage.RevokedDocument
import com.k689.identid.model.storage.TransactionLog
import com.k689.identid.storage.dao.BookmarkDao
import com.k689.identid.storage.dao.RevokedDocumentDao
import com.k689.identid.storage.dao.TransactionLogDao

@Database(
    entities = [
        Bookmark::class,
        RevokedDocument::class,
        TransactionLog::class,
    ],
    version = 1,
)
abstract class DatabaseService : RoomDatabase() {
    abstract fun bookmarkDao(): BookmarkDao

    abstract fun revokedDocumentDao(): RevokedDocumentDao

    abstract fun transactionLogDao(): TransactionLogDao
}
