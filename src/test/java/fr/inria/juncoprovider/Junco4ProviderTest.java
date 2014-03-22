package fr.inria.juncoprovider;

import junit.framework.Assert;
import org.apache.maven.surefire.booter.BaseProviderFactory;
import org.apache.maven.surefire.testset.TestRequest;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Properties;

/**
 * Created by marcel on 16/03/14.
 */

public class Junco4ProviderTest {

    @Ignore
    @Test
    public void testCreateProvider()
    {
        BaseProviderFactory providerParameters = new BaseProviderFactory( null, Boolean.TRUE );
        providerParameters.setProviderProperties( new Properties() );
        providerParameters.setClassLoaders( this.getClass().getClassLoader() );
        providerParameters.setTestRequest( new TestRequest( null, null, null ) );
        Assert.assertNotNull( new Junco4Provider(providerParameters) );
    }
}
