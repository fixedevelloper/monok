package com.monokek;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

/**
 * Verifies the module structure declared via {@code package-info.java}: no
 * cyclic dependencies, no module reaching into another module's internals.
 * Cross-module foreign keys are plain {@code Long} ids on purpose (see each
 * module's package-info), so this should stay green as new modules are
 * fleshed out with actual business logic.
 */
class ModularityTests {

    ApplicationModules modules = ApplicationModules.of(MonokekApplication.class);

    @Test
    void verifiesModularStructure() {
        modules.verify();
    }

    @Test
    void writesDocumentationSnapshot() {
        new Documenter(modules)
                .writeDocumentation()
                .writeIndividualModulesAsPlantUml();
    }
}
