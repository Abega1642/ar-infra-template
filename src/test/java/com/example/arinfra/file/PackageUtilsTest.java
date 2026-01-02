package com.example.arinfra.file;

import static com.example.arinfra.file.PackageUtils.getAncestorPackage;
import static com.example.arinfra.file.PackageUtils.getGrandparentPackage;
import static com.example.arinfra.file.PackageUtils.getPackage;
import static com.example.arinfra.file.PackageUtils.getPackageDepth;
import static com.example.arinfra.file.PackageUtils.getParentPackage;
import static com.example.arinfra.file.PackageUtils.isDirectChild;
import static com.example.arinfra.file.PackageUtils.isWithinPackageHierarchy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.arinfra.InfraGenerated;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;

@InfraGenerated
class PackageUtilsTest {

  @Test
  void should_get_package_from__spring_boot_class() {
    String packageName = getPackage(SpringApplication.class);
    assertEquals("org.springframework.boot", packageName);
  }

  @Test
  void should_get_package_from_spring_context_class() {
    String packageName = getPackage(ApplicationContext.class);
    assertEquals("org.springframework.context", packageName);
  }

  @Test
  void should_get_parent_package_from_class() {
    String parentPackage = getParentPackage(RestController.class);
    assertEquals("org.springframework.web.bind", parentPackage);
  }

  @Test
  void should_get_parent_package_from_string() {
    String parentPackage = getParentPackage("org.springframework.boot.autoconfigure");
    assertEquals("org.springframework.boot", parentPackage);
  }

  @Test
  void should_return_empty_for_single_level_package() {
    String parentPackage = getParentPackage("org");
    assertEquals("", parentPackage);
  }

  @Test
  void should_return_empty_for_empty_package() {
    String parentPackage = getParentPackage("");
    assertEquals("", parentPackage);
  }

  @Test
  void should_get_grandparent_package_from_Class() {
    String grandparentPackage = getGrandparentPackage(Service.class);
    assertEquals("org", grandparentPackage);
  }

  @Test
  void should_get_grandparent_package_from_String() {
    String grandparentPackage = getGrandparentPackage("org.springframework.boot.autoconfigure");
    assertEquals("org.springframework", grandparentPackage);
  }

  @Test
  void should_get_ancestor_at_level_3() {
    String ancestor = getAncestorPackage(RestController.class, 3);
    assertEquals("org.springframework", ancestor);
  }

  @Test
  void should_get_ancestor_from_string_at_level_2() {
    String ancestor = getAncestorPackage("org.springframework.boot.autoconfigure.web", 2);
    assertEquals("org.springframework.boot", ancestor);
  }

  @Test
  void should_determine_correct_direct_child() {
    assertTrue(isDirectChild("org.springframework.boot", "org.springframework"));
  }

  @Test
  void should_return_empty_when_level_exceeds_depth() {
    String ancestor = getAncestorPackage("org.springframework", 5);
    assertEquals("", ancestor);
  }

  @Test
  void should_throw_exception_when_level_is_negative() {
    assertThrows(
        IllegalArgumentException.class, () -> getAncestorPackage(SpringApplication.class, -1));
  }

  @Test
  void should_calculate_depth_for_multilevel_package() {
    int depth = getPackageDepth("org.springframework.boot.autoconfigure");
    assertEquals(4, depth);
  }

  @Test
  void should_return_1_for_single_level_package() {
    int depth = getPackageDepth("org");
    assertEquals(1, depth);
  }

  @Test
  void should_return_0_for_empty_package() {
    int depth = getPackageDepth("");
    assertEquals(0, depth);
  }

  @Test
  void should_return_true_for_exact_package() {
    boolean result = isWithinPackageHierarchy(SpringApplication.class, "org.springframework.boot");
    assertTrue(result);
  }

  @Test
  void should_return_true_for_sub_package() {
    boolean result = isWithinPackageHierarchy(RestController.class, "org.springframework.web");
    assertTrue(result);
  }
}
