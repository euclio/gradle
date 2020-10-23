/*
 * Copyright 2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.internal.jvm.inspection;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import org.gradle.api.JavaVersion;
import org.gradle.internal.os.OperatingSystem;

import java.io.File;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.Set;

public interface JvmInstallationMetadata {

    enum JavaInstallationCapability {
        JAVA_COMPILER
    }

    static DefaultJvmInstallationMetadata from(File javaHome, JavaVersion version, String vendor, String implementationName) {
        return new DefaultJvmInstallationMetadata(javaHome, version, vendor, implementationName);
    }

    static JvmInstallationMetadata failure(File javaHome, String errorMessage) {
        return new FailureInstallationMetadata(javaHome, errorMessage);
    }

    JavaVersion getLangageVersion();

    JvmVendor getVendor();

    Path getJavaHome();

    String getDisplayName();

    Set<JavaInstallationCapability> getCapabilities();

    String getErrorMessage();

    class DefaultJvmInstallationMetadata implements JvmInstallationMetadata {

        private JavaVersion languageVersion;
        private final String vendor;
        private final String implementationName;
        private Path javaHome;
        private Supplier<Set<JavaInstallationCapability>> capabilities = Suppliers.memoize(() -> gatherCapabilities());

        private DefaultJvmInstallationMetadata(File javaHome, JavaVersion languageVersion, String vendor, String implementationName) {
            this.javaHome = javaHome.toPath();
            this.languageVersion = languageVersion;
            this.vendor = vendor;
            this.implementationName = implementationName;
        }

        @Override
        public JavaVersion getLangageVersion() {
            return languageVersion;
        }

        @Override
        public JvmVendor getVendor() {
            return JvmVendor.fromString(vendor);
        }

        @Override
        public Path getJavaHome() {
            return javaHome;
        }

        @Override
        public String getDisplayName() {
            final String vendor = determineVendorName();
            String installationType = determineInstallationType(vendor);
            return MessageFormat.format("{0}{1} {2}", vendor, installationType, getLangageVersion().getMajorVersion());
        }

        private String determineVendorName() {
            JvmVendor.KnownJvmVendor vendor = getVendor().getKnownVendor();
            if(vendor == JvmVendor.KnownJvmVendor.ORACLE) {
                if (implementationName != null && implementationName.contains("OpenJDK")) {
                    return "OpenJDK";
                }
            }
            return vendor.getDisplayName();
        }

        private String determineInstallationType(String vendor) {
            if (getCapabilities().contains(JavaInstallationCapability.JAVA_COMPILER)) {
                if (!vendor.toLowerCase().contains("jdk")) {
                    return " JDK";
                }
                return "";
            }
            return " JRE";
        }

        @Override
        public Set<JavaInstallationCapability> getCapabilities() {
            return capabilities.get();
        }

        private Set<JavaInstallationCapability> gatherCapabilities() {
            final File javaCompiler = new File(new File(javaHome.toFile(), "bin"), OperatingSystem.current().getExecutableName("javac"));
            if (javaCompiler.exists()) {
                return Collections.singleton(JavaInstallationCapability.JAVA_COMPILER);
            }
            return Collections.emptySet();
        }

        @Override
        public String getErrorMessage() {
            throw new UnsupportedOperationException();
        }

    }

    class FailureInstallationMetadata implements JvmInstallationMetadata {

        private final File javaHome;
        private final String errorMessage;

        private FailureInstallationMetadata(File javaHome, String errorMessage) {
            this.javaHome = javaHome;
            this.errorMessage = errorMessage;
        }

        @Override
        public JavaVersion getLangageVersion() {
            throw unsupportedOperation();
        }

        @Override
        public JvmVendor getVendor() {
            throw unsupportedOperation();
        }

        @Override
        public Path getJavaHome() {
            return javaHome.toPath();
        }

        @Override
        public String getDisplayName() {
            return "Invalid installation: " + getErrorMessage();
        }

        @Override
        public Set<JavaInstallationCapability> getCapabilities() {
            return Collections.emptySet();
        }

        private UnsupportedOperationException unsupportedOperation() {
            return new UnsupportedOperationException("Installation is not valid. Original error message: " + getErrorMessage());
        }

        @Override
        public String getErrorMessage() {
            return errorMessage;
        }

    }

}
