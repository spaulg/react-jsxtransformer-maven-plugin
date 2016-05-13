
/*
    Copyright 2014 Simon Paulger <spaulger@codezen.co.uk>

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
*/

package uk.co.codezen.maven.react.jsxtransformer.mojo;

import io.apigee.trireme.core.NodeEnvironment;
import io.apigee.trireme.core.NodeException;
import io.apigee.trireme.core.NodeScript;
import io.apigee.trireme.core.ScriptStatus;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.impl.StaticLoggerBinder;

import java.io.*;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

@Mojo(name = "compile", defaultPhase = LifecyclePhase.COMPILE)
final public class CompilerMojo extends AbstractMojo
{
    private static final int BUFFER_SIZE = 0x4000;

    protected Logger logger;

    @Parameter(defaultValue = "${project.build.directory}/react-jsxtransformer/")
    private String nodeModuleExtractPath;

    /**
     * Source path for transforming JSX
     */
    @Parameter(defaultValue = "${project.build.directory}/${project.build.finalName}")
    private String sourcePath;

    /**
     * Source encoding for JSX transformation
     */
    @Parameter(defaultValue = "utf8")
    private String sourceCharSet;

    /**
     * Target path for transforming JSX
     */
    @Parameter(defaultValue = "${project.build.directory}/${project.build.finalName}")
    private String targetPath;

    /**
     * Target encoding for JSX transformation
     */
    @Parameter(defaultValue = "utf8")
    private String targetCharSet;

    /**
     * Target ECMAScript version
     */
    @Parameter(defaultValue = "es5")
    private String targetEcmaScriptVersion;

    /**
     * Strip annotation types
     */
    @Parameter
    private boolean stripAnnotationTypes = false;

    /**
     * Rewrite all module identifiers to be relative
     */
    @Parameter
    private boolean relative = false;

    /**
     * Scan modules for required dependencies
     */
    @Parameter
    private boolean followRequires = false;

    /**
     * File extension to scan for
     */
    @Parameter
    private String extension = "js";

    /**
     * List of module IDs
     */
    @Parameter
    private List<String> moduleIds = new ArrayList<String>();



    /**
     * Constructor
     */
    public CompilerMojo()
    {
        StaticLoggerBinder.getSingleton().setMavenLog(this.getLog());
        this.logger = LoggerFactory.getLogger(this.getClass());
    }

    /**
     * Get node module extraction path
     *
     * @return Node module extraction path
     */
    public String getNodeModuleExtractPath()
    {
        return this.nodeModuleExtractPath;
    }

    /**
     * Set node module extraction path
     *
     * @param nodeModuleExtractPath Node module extraction path
     */
    public void setNodeModuleExtractPath(String nodeModuleExtractPath)
    {
        this.nodeModuleExtractPath = nodeModuleExtractPath;
    }

    /**
     * Get list of module IDs
     *
     * @return List of module IDs
     */
    public List<String> getModuleIds()
    {
        return this.moduleIds;
    }

    /**
     * Set list of module IDs
     *
     * @param moduleIds List of module IDs
     */
    public void setModuleIds(List<String> moduleIds)
    {
        this.moduleIds = moduleIds;
    }

    /**
     * Get source path
     *
     * @return Source path
     */
    public String getSourcePath()
    {
        return this.sourcePath;
    }

    /**
     * Set source path
     *
     * @param sourcePath Source path
     */
    public void setSourcePath(String sourcePath)
    {
        this.sourcePath = sourcePath;
    }

    /**
     * Get source character set
     *
     * @return Source character set
     */
    public String getSourceCharSet()
    {
        return this.sourceCharSet;
    }

    /**
     * Set source character set
     *
     * @param sourceCharSet Source character set
     */
    public void setSourceCharSet(String sourceCharSet)
    {
        this.sourceCharSet = sourceCharSet;
    }

    /**
     * Get target path
     *
     * @return Target path
     */
    public String getTargetPath()
    {
        return this.targetPath;
    }

    /**
     * Set target path
     *
     * @param targetPath Target path
     */
    public void setTargetPath(String targetPath)
    {
        this.targetPath = targetPath;
    }

    /**
     * Get target character set
     *
     * @return Target character set
     */
    public String getTargetCharSet()
    {
        return this.targetCharSet;
    }

    /**
     * Set target character set
     *
     * @param targetCharSet Target character set
     */
    public void setTargetCharSet(String targetCharSet)
    {
        this.targetCharSet = targetCharSet;
    }

    /**
     * Get target ECMAScript version
     *
     * @return ECMAScript version
     */
    public String getTargetEcmaScriptVersion()
    {
        return this.targetEcmaScriptVersion;
    }

    /**
     * Set target ECMAScript version
     *
     * @param targetEcmaScriptVersion ECMAScript version
     */
    public void setTargetEcmaScriptVersion(String targetEcmaScriptVersion)
    {
        this.targetEcmaScriptVersion = targetEcmaScriptVersion;
    }

    /**
     * If stripping annotation types
     *
     * @return Stripping annotation types
     */
    public boolean isStripAnnotationTypes()
    {
        return this.stripAnnotationTypes;
    }

    /**
     * Set if stripping annotation types
     *
     * @param stripAnnotationTypes Stripping annotation types
     */
    public void setStripAnnotationTypes(boolean stripAnnotationTypes)
    {
        this.stripAnnotationTypes = stripAnnotationTypes;
    }

    /**
     * Make modules relative
     *
     * @return Make modules relative
     */
    public boolean isRelative()
    {
        return this.relative;
    }

    /**
     * Set make modules relative
     *
     * @param relative Relative modules
     */
    public void setRelative(boolean relative)
    {
        this.relative = relative;
    }

    /**
     * Follow dependencies
     *
     * @return If follow dependencies
     */
    public boolean isFollowRequires()
    {
        return this.followRequires;
    }

    /**
     * Set follow dependencies
     *
     * @param followRequires Follow dependencies
     */
    public void setFollowRequires(boolean followRequires)
    {
        this.followRequires = followRequires;
    }

    /**
     * Get the file scan extension
     *
     * @return File scan extension
     */
    public String getExtension()
    {
        return this.extension;
    }

    /**
     * Set the file scan extension
     *
     * @param extension File scan extension
     */
    public void setExtension(String extension)
    {
        this.extension = extension;
    }

    /**
     * Execute Mojo
     *
     * @throws MojoExecutionException
     * @throws MojoFailureException
     */
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException
    {
        NodeEnvironment env = new NodeEnvironment();
        ScriptStatus status;

        this.logger.info("Extracting React tools and dependencies...");
        this.extractNodeModules(this.getNodeModuleExtractPath());

        String jsxBin = String.format("%sreact-tools%sbin%sjsx",
            this.nodeModuleExtractPath, File.separator, File.separator);
        this.logger.info(String.format("jsx transformer binary is available at %s", jsxBin));

        try {
            this.logger.info(String.format("Transforming files in source path '%s' to destination path '%s'",
                this.getSourcePath(), this.getTargetPath()));

            String jsxCommand = "";
            String[] commandArgs = this.buildCommand(jsxBin);
            for (String commandArg : commandArgs) {
                jsxCommand = jsxCommand + commandArg + " ";
            }
            this.logger.debug(String.format("Executing jsx binary command: %s", jsxCommand.trim()));

            NodeScript script = env.createScript(commandArgs, false);
            script.setWorkingDirectory(this.nodeModuleExtractPath);
            status = script.execute().get();
        }
        catch (NodeException ex) {
            throw new MojoExecutionException("Exception raised whilst executing JSX transformer", ex);
        }
        catch (InterruptedException ex) {
            throw new MojoExecutionException("Exception raised whilst executing JSX transformer", ex);
        }
        catch (ExecutionException ex) {
            throw new MojoExecutionException("Exception raised whilst executing JSX transformer", ex);
        }

        if (null != status) {
            if (0 != status.getExitCode()) {
                throw new MojoExecutionException("JSX transformer returned non zero status code", status.getCause());
            }
        }
    }

    /**
     * Extract node modules
     *
     * @param destinationPath Destination extraction path
     * @throws MojoExecutionException
     */
    private void extractNodeModules(String destinationPath) throws MojoExecutionException
    {
        // Discover path to JAR file for extraction
        String jarPath;

        try {
            jarPath = this.getClass().getProtectionDomain().getCodeSource().getLocation().toURI().getPath();

            if (null == jarPath) {
                throw new MojoExecutionException("Failed to discover JAR path to extract embedded node modules");
            }


        }
        catch (URISyntaxException ex) {
            throw new MojoExecutionException("Failed to discover JAR path to extract embedded node modules", ex);
        }

        // Extract node modules from JAR
        try {
            InputStream jarEntryStream = null;
            FileOutputStream outputFileStream = null;
            JarFile jarFile = null;
            byte[] buffer = new byte[BUFFER_SIZE];

            try {
                jarFile = new JarFile(jarPath);
                Enumeration<JarEntry> enumEntries = jarFile.entries();

                while (enumEntries.hasMoreElements()) {
                    JarEntry entry = enumEntries.nextElement();
                    String entryFileName = entry.getName();

                    if (entryFileName.startsWith("META-INF/node_modules/") && ! entryFileName.equals("META-INF/node_modules/")) {
                        // Limit extraction to node modules only
                        entryFileName = entryFileName.replaceFirst("META-INF\\/node_modules\\/", "");
                        File outputFile = new File(destinationPath + File.separator + entryFileName);

                        this.logger.debug(String.format("Extracting JAR file '%s' to destination '%s'",
                            entryFileName, outputFile.getCanonicalPath()));

                        if (entry.isDirectory()) {
                            if ( ! outputFile.mkdirs() && ! outputFile.exists()) {
                                // Failed to create parent directories
                                throw new MojoExecutionException(String.format("Failed to create path '%s' " +
                                    "whilst extracting embedded node modules", outputFile.getCanonicalPath()));
                            }
                        }
                        else {
                            jarEntryStream = jarFile.getInputStream(entry);
                            outputFileStream = new FileOutputStream(outputFile);

                            while (true) {
                                int bytesRead = jarEntryStream.read(buffer);
                                if (bytesRead == -1) {
                                    break;
                                }
                                outputFileStream.write(buffer, 0, bytesRead);
                            }

                            jarEntryStream.close();
                            outputFileStream.close();
                        }
                    }
                }

                jarFile.close();
            }
            catch (IOException ex) {
                throw new MojoExecutionException("Failed to open JAR path to extract embedded node modules", ex);
            }
            finally {
                if (null != outputFileStream) {
                    outputFileStream.close();
                }

                if (null != jarEntryStream) {
                    jarEntryStream.close();
                }

                if (null != jarFile) {
                    jarFile.close();
                }
            }
        }
        catch (IOException ex) {
            throw new MojoExecutionException("Failed to open JAR path to extract embedded node modules", ex);
        }
    }

    /**
     * Construct the JSX command to execute
     *
     * @param scriptName Script name
     * @return JSX command
     */
    private String[] buildCommand(String scriptName)
    {
        List<String> command = new ArrayList<String>();
        command.add(scriptName);

        if (this.isRelative()) {
            command.add("--relativize");
        }

        if (this.isFollowRequires()) {
            command.add("--follow-requires");
        }

        if ( ! this.getSourceCharSet().equals("")) {
            command.add("--source-charset");
            command.add("utf8");
        }

        if ( ! this.getTargetCharSet().equals("")) {
            command.add("--output-charset");
            command.add("utf8");
        }

        if ( ! this.getTargetEcmaScriptVersion().equals("")) {
            command.add("--target");
            command.add("es5");
        }

        if (this.isStripAnnotationTypes()) {
            command.add("--strip-types");
        }

        command.add("--extension");
        command.add(this.getExtension());

        command.add(this.getSourcePath());
        command.add(this.getTargetPath());
        command.addAll(this.getModuleIds());

        return command.toArray(new String[] {});
    }
}
