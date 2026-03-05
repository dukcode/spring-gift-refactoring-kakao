package gift;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

@AnalyzeClasses(packages = "gift", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule controllers_should_not_depend_on_repositories =
        noClasses().that().haveSimpleNameEndingWith("Controller")
            .should().dependOnClassesThat().haveSimpleNameEndingWith("Repository");

    @ArchTest
    static final ArchRule services_should_not_depend_on_other_domain_repositories =
        classes().that().haveSimpleNameEndingWith("Service")
            .should(new ArchCondition<JavaClass>("not depend on repositories from other packages") {
                @Override
                public void check(JavaClass clazz, ConditionEvents events) {
                    clazz.getDirectDependenciesFromSelf().stream()
                        .filter(dep -> dep.getTargetClass().getSimpleName().endsWith("Repository"))
                        .filter(dep -> !dep.getTargetClass().getPackageName().equals(clazz.getPackageName()))
                        .forEach(dep -> events.add(SimpleConditionEvent.violated(
                            clazz,
                            clazz.getName() + " depends on " + dep.getTargetClass().getName()
                                + " from a different package"
                        )));
                }
            });
}
