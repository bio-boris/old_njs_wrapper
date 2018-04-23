package us.kbase.narrativejobservice.test;

import org.ini4j.InvalidFileFormatException;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import us.kbase.auth.AuthToken;
import us.kbase.common.service.JsonClientException;
import us.kbase.common.service.UObject;
import us.kbase.narrativejobservice.sdkjobs.SDKMethodRunner;
import us.kbase.narrativejobservice.test.TesterUtils;
import us.kbase.workspace.CreateWorkspaceParams;
import us.kbase.workspace.WorkspaceClient;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Properties;
import java.util.Map;


public class SDKMethodRunnerTest {

    static AuthToken token;
    static String njs_url;
    static Map<String, String> config;
    static long testWsID ;

    public String lookupServiceVersion(String moduleName) throws Exception,
            IOException, InvalidFileFormatException, JsonClientException {
        us.kbase.catalog.CatalogClient cat = getCatalogClient(token, TesterUtils.loadConfig());
        String ver = cat.getModuleInfo(new us.kbase.catalog.SelectOneModuleParams().withModuleName(moduleName)).getDev().getGitCommitHash();
        return ver;
    }

    private static us.kbase.catalog.CatalogClient getCatalogClient(AuthToken auth,
                                                                   Map<String, String> config) throws Exception {
        String catUrl = config.get(us.kbase.narrativejobservice.NarrativeJobServiceServer.CFG_PROP_CATALOG_SRV_URL);
        us.kbase.catalog.CatalogClient ret = new us.kbase.catalog.CatalogClient(new URL(catUrl), auth);
        ret.setAllSSLCertificatesTrusted(true);
        ret.setIsInsecureHttpConnectionAllowed(true);
        return ret;
    }


    private static WorkspaceClient getWsClient(AuthToken auth,
                                               Map<String, String> config) throws Exception {
        String wsUrl = config.get(us.kbase.narrativejobservice.NarrativeJobServiceServer.CFG_PROP_WORKSPACE_SRV_URL);
        WorkspaceClient ret = new WorkspaceClient(new URL(wsUrl), auth);
        ret.setIsInsecureHttpConnectionAllowed(true);
        return ret;
    }

    private static void setupWorkSpace() throws Exception{
        WorkspaceClient wscl = getWsClient(token,config);

        String machineName = java.net.InetAddress.getLocalHost().getHostName();
        machineName = machineName == null ? "nowhere" : machineName.toLowerCase().replaceAll("[^\\dA-Za-z_]|\\s", "_");
        long suf = System.currentTimeMillis();


        Exception error = null;
        for (int i = 0; i < 5; i++) {
            String testWsName = "test_awe_docker_job_script_" + machineName + "_" + suf;
            try {
                testWsID = wscl.createWorkspace(new CreateWorkspaceParams()
                        .withWorkspace(testWsName)).getE1();
                error = null;
                break;
            } catch (Exception ex) {
                System.err.println(ex.getMessage());
                error = ex;
            }
        }

    }

    @BeforeClass
    public static void setUpStuff() throws Exception {
        Properties props = TesterUtils.props();
        token = TesterUtils.token(props);
        njs_url = props.getProperty("njs_server_url");
        config = TesterUtils.loadConfig();
        setupWorkSpace();


    }


    //Atempting to use Mockito and Mockito Spies

    @Test
    public void testRunSimpleApp() throws Exception {

        String moduleName = "simpleapp";
        String methodName = "simple_add";

        String jsonInput = "{\"base_number\":\"101\"}";
        String serviceVer = lookupServiceVersion(moduleName);

        Map<String, String> meta = new HashMap<String, String>();
        meta.put("foo", "bar");

        us.kbase.narrativejobservice.RunJobParams params = new us.kbase.narrativejobservice.RunJobParams().withMethod(
                moduleName + "." + methodName).withServiceVer(serviceVer)
                .withAppId("myapp/foo").withMeta(meta).withWsid(testWsID)
                .withParams(Arrays.asList(UObject.fromJsonString(jsonInput)));


        String appJobId = "appJobID";
        String aweClientGroups = "ci";

        String ret = us.kbase.narrativejobservice.sdkjobs.SDKMethodRunner.runJob(params, token, appJobId, config, aweClientGroups);
        assert(true);
    }


}
