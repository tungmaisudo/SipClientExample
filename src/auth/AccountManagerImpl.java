package auth;


import gov.nist.javax.sip.clientauthutils.AccountManager;
import gov.nist.javax.sip.clientauthutils.UserCredentials;

import javax.sip.ClientTransaction;

public class AccountManagerImpl implements AccountManager {

    private String userName;
    private String sipDomain;
    private String password;

    public AccountManagerImpl(String userName, String sipDomain, String password) {
        this.userName = userName;
        this.sipDomain = sipDomain;
        this.password = password;
    }

    public UserCredentials getCredentials(ClientTransaction challengedTransaction, String realm) {
        return new UserCredentialsImpl(userName,sipDomain,password);
    }


}