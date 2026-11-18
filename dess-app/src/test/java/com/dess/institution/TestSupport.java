package com.dess.institution;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * @brief Test utilities for XML resources and disposable copies.
 *
 * <p>The original resource files are never modified; tests always work on copies placed in a
 * temporary directory.</p>
 */
public final class TestSupport
{
    private TestSupport()
    {
    }

    /**
     * Resolves a classpath resource to a file system path.
     *
     * @param relativePath resource path relative to the classpath root
     * @return the resolved path
     * @throws IllegalArgumentException if the resource does not exist
     */
    public static Path resource(String relativePath)
    {
        try
        {
            var url = TestSupport.class.getResource("/" + relativePath);

            if (url == null)
            {
                throw new IllegalArgumentException("Test resource not found: " + relativePath);
            }

            return Path.of(url.toURI());
        }
        catch (URISyntaxException e)
        {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Copies a resource, for example {@code valid/university.xml}, to {@code dir/institution.xml}.
     *
     * @param dir          target directory
     * @param resourcePath resource path relative to the classpath root
     * @return the path of the copied file
     */
    public static Path copyTo(Path dir, String resourcePath)
    {
        try (InputStream in = Files.newInputStream(resource(resourcePath)))
        {
            Path target = dir.resolve("institution.xml");
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);

            return target;
        }
        catch (IOException e)
        {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Opens the module on a temporary copy of the supplied resource.
     *
     * @param dir          directory receiving the copy
     * @param resourcePath resource path relative to the classpath root
     * @return the opened module
     */
    public static InstitutionModule openCopy(Path dir, String resourcePath)
    {
        return InstitutionModule.open(copyTo(dir, resourcePath));
    }

    /**
     * Opens the module on a temporary copy of the university example.
     *
     * @param dir directory receiving the copy
     * @return the opened module
     */
    public static InstitutionModule openUniversity(Path dir)
    {
        return openCopy(dir, "valid/university.xml");
    }
}
