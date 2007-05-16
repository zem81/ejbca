package org.ejbca.ui.web.pub.retrieve;

import java.math.BigInteger;
import java.rmi.RemoteException;
import java.util.Collection;

import javax.ejb.CreateException;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.rmi.PortableRemoteObject;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Hex;
import org.ejbca.core.ejb.ca.caadmin.ICAAdminSessionHome;
import org.ejbca.core.ejb.ca.caadmin.ICAAdminSessionRemote;
import org.ejbca.core.ejb.ca.sign.ISignSessionHome;
import org.ejbca.core.ejb.ca.sign.ISignSessionRemote;
import org.ejbca.core.ejb.ca.store.ICertificateStoreSessionHome;
import org.ejbca.core.ejb.ca.store.ICertificateStoreSessionRemote;
import org.ejbca.core.model.ca.caadmin.CAInfo;
import org.ejbca.core.model.ca.crl.RevokedCertInfo;
import org.ejbca.core.model.log.Admin;

/**
 * This bean performs a number of certificate searches for the public web.
 * 
 * To make it easy to use from JSTL pages, most methods take no arguments.
 * The arguments are supplied as member variables instead. <br>
 * 
 * @author Rolf Staflin
 * @version $Id: CertificateFinderBean.java,v 1.3 2007-05-16 06:43:21 rolf_s Exp $
 */
public class CertificateFinderBean {
	
	private static final Logger log = Logger.getLogger(CertificateFinderBean.class);

	private ISignSessionRemote mSignSession;
	private ICAAdminSessionRemote mCaAdminSession;
	private ICertificateStoreSessionRemote mStoreSession;
	private boolean mInitialized = false;
	private Admin mAdmin;
	
	/** This member is used by the JSP pages to indicate which CA they are interested in. 
	 * It is used by getCAInfo().
	 */
	private int mCurrentCA;


	/**
	 * Empty default constructor.
	 * NOTE: Call initialize() after creating this object.
	 */
	public CertificateFinderBean() {
	}
	
	/**
	 * Initializes all the session beans used by this ocject.
	 * This method must be called before other methods.
	 * <p>Call it like this:
	 * <br><tt>&lt;% finder.initialize(request.getRemoteAddr()); %&gt</tt>
	 * 
	 * @param remoteAddress The remote address as supplied by the request JSP object.
	 * @throws NamingException If context related errors occur.
	 * @throws RemoteException If session bean creation fails.
	 * @throws CreateException If session bean creation fails.
	 */
	public void initialize(String remoteAddress) throws NamingException, RemoteException, CreateException {
		log.info(">initialize()");
	    mAdmin = new Admin(Admin.TYPE_PUBLIC_WEB_USER, remoteAddress);
		InitialContext ctx = new InitialContext();
	    final ISignSessionHome home = (ISignSessionHome) PortableRemoteObject.narrow(ctx.lookup("RSASignSession"), ISignSessionHome.class );
	    mSignSession = home.create();
		ICAAdminSessionHome cahome = (ICAAdminSessionHome) PortableRemoteObject.narrow(ctx.lookup("CAAdminSession"), ICAAdminSessionHome.class );            
	    mCaAdminSession = cahome.create();
        ICertificateStoreSessionHome cshome = (ICertificateStoreSessionHome) 
        		PortableRemoteObject.narrow(ctx.lookup("CertificateStoreSession"), ICertificateStoreSessionHome.class );
        mStoreSession = cshome.create();

	    mInitialized = true;
	}

	public Collection getAvailableCAs() throws RemoteException {
		log.info(">getAvailableCAs()");
		return mInitialized ? mCaAdminSession.getAvailableCAs(mAdmin) : null;
	}

	public int getCurrentCA() {
		return mCurrentCA;
	}

	public void setCurrentCA(Integer currentCA) {
		mCurrentCA = currentCA;
	}

	public CAInfo getCAInfo() throws RemoteException {
		log.info(">getCAInfo() currentCA = " + mCurrentCA + ", initialized == " + mInitialized);
		return mInitialized ? mCaAdminSession.getCAInfo(mAdmin, mCurrentCA) : null;
	}

	public Collection getCACertificateChain() throws RemoteException {
		log.info(">getCACertificateChain() currentCA = " + mCurrentCA + ", initialized == " + mInitialized);
		return mInitialized ? mSignSession.getCertificateChain(mAdmin, mCurrentCA) : null;
	}
	
	/**
	 * Get revocation info for a certificate.
	 * This method fills in the supplied RevokedCertInfo object with data about a certificate.
	 * Since Java uses "call by reference" this works fine, but we can't create our own object because
	 * the caller doesn't read the reference when unwinding the stack after this method returns.
	 * 
	 * @param issuerDN DN of the certificate's issuer
	 * @param serialNumber The serial number of the certificate
	 * @param result An allocated object. Data about the certificate is entered in the result object by this method.
	 *        If no info can be found (e.g., if the certificate does not exist), the revocationDate and
	 *        userCertificate fields of result are set to null. 
	 * @throws RemoteException If a communication error occurs while looking up the info.
	 */
	public void lookupRevokedInfo(String issuerDN, String serialNumber, RevokedCertInfo result) throws RemoteException {
		if (result == null) {
			return; // There's nothing we can do here.
		}
		BigInteger serialBignum = new BigInteger(Hex.decode(StringUtils.trimToEmpty(serialNumber)));
		RevokedCertInfo info = mStoreSession.isRevoked(mAdmin, StringUtils.trimToEmpty(issuerDN), serialBignum);
		
		if (info == null) {
			result.setRevocationDate(null);
			result.setUserCertificate(null);
		} else {
			result.setReason(info.getReason());
			result.setRevocationDate(info.getRevocationDate());
			result.setUserCertificate(info.getUserCertificate());
		}
	}
}
