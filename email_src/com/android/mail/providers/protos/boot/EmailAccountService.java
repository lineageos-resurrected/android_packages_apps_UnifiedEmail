/**
 * Copyright (c) 2011, Google Inc.
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
package com.android.mail.providers.protos.boot;

import android.app.IntentService;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;

import com.android.emailcommon.provider.Account;
import com.android.emailcommon.provider.EmailContent;
import com.android.mail.providers.AccountCacheProvider;
import com.android.mail.providers.UIProvider.AccountCapabilities;
import com.android.mail.providers.UIProvider.AccountColumns;
import com.android.mail.providers.protos.mock.MockUiProvider;

import java.util.Map;

/**
 * A service to handle various intents asynchronously.
 */
public class EmailAccountService extends IntentService {
    private static final long BASE_EAS_CAPABILITIES =
        AccountCapabilities.SYNCABLE_FOLDERS |
        AccountCapabilities.FOLDER_SERVER_SEARCH |
        AccountCapabilities.SANITIZED_HTML |
        AccountCapabilities.SMART_REPLY |
        AccountCapabilities.SERVER_SEARCH |
        AccountCapabilities.UNDO;

    private static final Uri BASE_SETTINGS_URI =
            Uri.parse("content://ui.email.android.com/settings");

    private static String getUriString(String type, long accountId) {
        return EmailContent.CONTENT_URI.toString() + "/" + type + "/" + accountId;
    }

    private static Uri getAccountSettingUri(String account) {
        return BASE_SETTINGS_URI.buildUpon().appendQueryParameter("account", account).build();
    }

    public EmailAccountService() {
        super("EmailAccountService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        final String action = intent.getAction();
        if (AccountReceiver.ACTION_PROVIDER_CREATED.equals(action)) {
            // Register all Email accounts
            getAndRegisterEmailAccounts();
        }
    }

    private void getAndRegisterEmailAccounts() {
        // Use EmailProvider to get our accounts
        Cursor c = getContentResolver().query(Account.CONTENT_URI, Account.CONTENT_PROJECTION, null,
                null, null);
        if (c == null) return;
        try {
            int i = 0;
            while (c.moveToNext()) {
                final Map<String, Object> mockAccountMap =
                    MockUiProvider.createAccountDetailsMap(i % MockUiProvider.NUM_MOCK_ACCOUNTS,
                            false);
                // Send our account information to the cache provider
                String accountName = c.getString(Account.CONTENT_EMAIL_ADDRESS_COLUMN);
                long id = c.getLong(Account.CONTENT_ID_COLUMN);
                final AccountCacheProvider.CachedAccount cachedAccount =
                    new AccountCacheProvider.CachedAccount(
                            id,
                            accountName,
                            getUriString("uiaccount", id),
                            // TODO: Check actual account protocol and return proper values
                            BASE_EAS_CAPABILITIES,
                            getUriString("uifolders", id),
                            (String)mockAccountMap.get(AccountColumns.SEARCH_URI),
                            (String)mockAccountMap.get(AccountColumns.ACCOUNT_FROM_ADDRESSES_URI),
                            getUriString("uisavedraft", id),
                            getUriString("uisendmail", id),
                            (String)mockAccountMap.get(AccountColumns.EXPUNGE_MESSAGE_URI),
                            getUriString("uiundo", id),
                            getAccountSettingUri(accountName).toString(),
                            null /* currently no help content */,
                            0);

                AccountCacheProvider.addAccount(cachedAccount);
                i++;
            }
        } finally {
            c.close();
        }
    }
}