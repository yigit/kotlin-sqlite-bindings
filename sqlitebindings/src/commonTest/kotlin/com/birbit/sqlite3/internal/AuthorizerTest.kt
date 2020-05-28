/*
 * Copyright 2020 Google, LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.birbit.sqlite3.internal

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AuthorizerTest {
    private class TestAuthorizer : Authorizer {
        var disposed = false
        var invoked = false
        override fun invoke(params: AuthorizationParams): AuthResult {
            check(!disposed) {
                "got invocation call after dispose"
            }
            invoked = true
            return AuthResult.OK
        }

        override fun dispose() {
            disposed = true
        }
    }

    @Test
    fun disposePreviousAuthorizer() {
        val db = SqliteApi.openConnection(":memory:")
        fun runQuery() {
            val stmt = SqliteApi.prepareStmt(db, "SELECT * FROM sqlite_master")
            SqliteApi.finalize(stmt)
        }
        try {
            val auth1 = TestAuthorizer()
            val auth2 = TestAuthorizer()
            SqliteApi.setAuthorizer(db, auth1)
            runQuery()
            assertTrue(auth1.invoked, "should have invoked authorizer")
            assertFalse(auth1.disposed, "should not be disposed yet")
            auth1.invoked = false
            SqliteApi.setAuthorizer(db, auth2)
            assertTrue(auth1.disposed, "should've disposed auth 1")
            runQuery()
            assertTrue(auth2.invoked, "should have invoked authorizer")
            assertFalse(auth2.disposed, "should not be disposed yet")
            assertFalse(auth1.invoked, "should not call previous authorizer")
        } finally {
            SqliteApi.close(db)
        }
    }
}
