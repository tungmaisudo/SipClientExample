package jain;


import gov.nist.javax.sip.clientauthutils.AccountManager;
import gov.nist.javax.sip.clientauthutils.UserCredentials;

import javax.sip.ClientTransaction;

public class AccountManagerImpl implements AccountManager {


    public UserCredentials getCredentials(ClientTransaction challengedTransaction, String realm) {
        return new UserCredentialsImpl("milete02","sip.linphone.org","milete02");
    }

}