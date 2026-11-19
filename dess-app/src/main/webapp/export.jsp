<%--
    Streams the persisted institution configuration as an XML download.

    The file is the exact artifact written by the institution module from the named tree. The
    download name uses the tree business code so the exported file is easy to identify.
--%>
<%@ page contentType="application/xml; charset=UTF-8" pageEncoding="UTF-8"
         session="false" trimDirectiveWhitespaces="true" %>
<%@ page import="com.dess.institution.InstitutionSettings" %>
<%@ page import="com.dess.institution.application.ConfigurationStatus" %>
<%@ page import="com.dess.institution.application.InstitutionInfo" %>
<%@ page import="com.dess.institution.application.InstitutionService" %>
<%@ page import="jakarta.enterprise.inject.spi.CDI" %>
<%@ page import="java.io.IOException" %>
<%@ page import="java.io.Reader" %>
<%@ page import="java.nio.charset.StandardCharsets" %>
<%@ page import="java.nio.file.Files" %>
<%@ page import="java.nio.file.Path" %>
<%
    response.setHeader("Cache-Control", "no-store");
    response.setHeader("X-Content-Type-Options", "nosniff");

    Path exportFile;

    try
    {
        exportFile = InstitutionSettings.fromEnvironment().configFile().toAbsolutePath().normalize();
    }
    catch (RuntimeException error)
    {
        response.setStatus(500);
        response.setContentType("text/plain; charset=UTF-8");
        out.clearBuffer();
        out.println("The institution configuration file could not be resolved: " + error.getMessage());
        return;
    }

    if (!Files.isRegularFile(exportFile))
    {
        response.setStatus(404);
        response.setContentType("text/plain; charset=UTF-8");
        out.clearBuffer();
        out.println("No institution configuration file: " + exportFile);
        return;
    }

    String filename = exportFile.getFileName().toString();

    try
    {
        InstitutionService institutionService = CDI.current().select(InstitutionService.class).get();

        if (institutionService.getStatus() == ConfigurationStatus.READY)
        {
            InstitutionInfo info = institutionService.getInfo();

            if (info.code() != null && !info.code().isBlank())
            {
                filename = info.code().replaceAll("[^A-Za-z0-9_.-]", "_") + ".xml";
            }
        }
    }
    catch (RuntimeException ignored)
    {
        // The file name is only cosmetic: fall back to the persisted file name.
    }

    response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");

    try (Reader reader = Files.newBufferedReader(exportFile, StandardCharsets.UTF_8))
    {
        out.clearBuffer();
        reader.transferTo(out);
        out.flush();
    }
    catch (IOException error)
    {
        response.setStatus(500);
        response.setContentType("text/plain; charset=UTF-8");
        out.println("The institution configuration file could not be read: " + error.getMessage());
    }

    return;
%>
