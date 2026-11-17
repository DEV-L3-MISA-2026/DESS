package mg.dess.platform;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

/** Entry point of the REST API: http://localhost:8080/dess/api */
@ApplicationPath("/api")
public class RestApplication extends Application {
}
