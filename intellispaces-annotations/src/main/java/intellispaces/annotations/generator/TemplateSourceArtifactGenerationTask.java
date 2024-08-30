package intellispaces.annotations.generator;

import intellispaces.annotations.artifact.Artifact;
import intellispaces.annotations.artifact.SourceArtifactImpl;
import intellispaces.commons.exception.UnexpectedViolationException;
import intellispaces.commons.function.Functions;
import intellispaces.commons.resource.ResourceFunctions;
import intellispaces.javastatements.customtype.CustomType;
import intellispaces.templates.TemplateEngine;
import intellispaces.templates.template.Template;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.processing.RoundEnvironment;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Template based source artifact generation task.
 */
public abstract class TemplateSourceArtifactGenerationTask implements GenerationTask {
  protected final CustomType initiatorType;
  protected final CustomType annotatedType;

  private static final Map<String, Template> TEMPLATE_CACHE = new HashMap<>();
  private static final Logger LOG = LoggerFactory.getLogger(TemplateSourceArtifactGenerationTask.class);

  public TemplateSourceArtifactGenerationTask(CustomType initiatorType, CustomType annotatedType) {
    this.initiatorType = initiatorType;
    this.annotatedType = annotatedType;
  }

  /**
   * Template name.
   */
  protected abstract String templateName();

  /**
   * Template variables.
   */
  protected abstract Map<String, Object> templateVariables();

  /**
   * Analyzes type and returns <code>true</code> if artifact should be created or <code>false</code> otherwise.
   */
  protected abstract boolean analyzeAnnotatedType(RoundEnvironment roundEnv);

  @Override
  public CustomType initiatorType() {
    return initiatorType;
  }

  @Override
  public CustomType annotatedType() {
    return annotatedType;
  }

  @Override
  public Optional<Artifact> execute(RoundEnvironment roundEnv) throws Exception {
    LOG.debug("Annotation processor generator " + this.getClass().getSimpleName() +
        ". Process class " + annotatedType.canonicalName() +
        ". Generate class " + artifactName());

    if (!analyzeAnnotatedType(roundEnv)) {
      return Optional.empty();
    }
    String source = synthesizeArtifact();
    return Optional.of(new SourceArtifactImpl(artifactName(), source));
  }

  private String synthesizeArtifact() throws Exception {
    Template template = TEMPLATE_CACHE.computeIfAbsent(templateName(),
        Functions.coveredThrowableFunction(this::makeTemplate)
    );
    return template.resolve(templateVariables());
  }

  private Template makeTemplate(String templateName) throws Exception {
    String templateSource = ResourceFunctions.readResourceAsString(
        TemplateSourceArtifactGenerationTask.class, templateName()
    ).orElseThrow(() -> UnexpectedViolationException.withMessage(
        "Template for generate artifact is not found. Template name {0}", templateName())
    );
    return TemplateEngine.parseTemplate(templateSource);
  }
}