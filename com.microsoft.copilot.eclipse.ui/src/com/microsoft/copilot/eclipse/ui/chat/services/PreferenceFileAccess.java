// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.chat.services;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileOwnerAttributeView;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;

/**
 * Error-propagating preference I/O that never truncates the saved document.
 *
 * <p>On POSIX providers without an ACL view, Java cannot preserve extended ACLs. Replacement intentionally narrows
 * group/ACL sharing by removing all group and other permission bits while retaining owner permissions, owner and
 * group identity. Preference sharing between instances running as the same OS user is preserved.
 */
final class PreferenceFileAccess implements PreferenceStorage.FileAccess {
  private final SnapshotWriter writer;
  private final AtomicReplacement replacement;

  PreferenceFileAccess() {
    this(Files::writeString, (source, target) ->
        Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING));
  }

  PreferenceFileAccess(SnapshotWriter writer, AtomicReplacement replacement) {
    this.writer = writer;
    this.replacement = replacement;
  }

  @Override
  public String read(Path path) throws IOException {
    return Files.readString(path);
  }

  @Override
  public void write(Path path, String content) throws IOException {
    Path target = path.toAbsolutePath();
    Files.createDirectories(target.getParent());
    Path temporary = Files.createTempFile(target.getParent(), ".copilot-preferences-", ".tmp");
    try {
      preserveProtections(target, temporary);
      writer.write(temporary, content);
      replacement.replace(temporary, target);
    } catch (IOException | RuntimeException | Error failure) {
      try {
        Files.deleteIfExists(temporary);
      } catch (IOException | RuntimeException cleanupFailure) {
        failure.addSuppressed(cleanupFailure);
      }
      throw failure;
    }
    Files.deleteIfExists(temporary);
  }

  private void preserveProtections(Path target, Path temporary) throws IOException {
    BasicFileAttributes attributes;
    try {
      attributes = Files.readAttributes(target, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
    } catch (NoSuchFileException missing) {
      return;
    }
    // Replacing a symlink would silently change its meaning and could discard its destination's protections.
    if (!attributes.isRegularFile()) {
      throw new IOException("Preference target is not a regular file: " + target);
    }

    PosixFileAttributeView posix = Files.getFileAttributeView(target, PosixFileAttributeView.class);
    AclFileAttributeView acl = Files.getFileAttributeView(target, AclFileAttributeView.class);
    if (posix != null) {
      var original = posix.readAttributes();
      var permissions = EnumSet.noneOf(PosixFilePermission.class);
      permissions.addAll(original.permissions());
      if (acl == null) {
        // Zeroing the POSIX group/ACL mask prevents inherited named-user entries from widening access.
        permissions.retainAll(EnumSet.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE,
            PosixFilePermission.OWNER_EXECUTE));
      }
      var destination = Files.getFileAttributeView(temporary, PosixFileAttributeView.class);
      var current = destination.readAttributes();
      if (!original.owner().equals(current.owner())) {
        destination.setOwner(original.owner());
      }
      if (!original.group().equals(current.group())) {
        destination.setGroup(original.group());
      }
      destination.setPermissions(permissions);
    }

    if (acl != null) {
      var destination = Files.getFileAttributeView(temporary, AclFileAttributeView.class);
      if (!acl.getOwner().equals(destination.getOwner())) {
        destination.setOwner(acl.getOwner());
      }
      destination.setAcl(acl.getAcl());
    } else if (posix == null) {
      FileOwnerAttributeView owner = Files.getFileAttributeView(target, FileOwnerAttributeView.class);
      if (owner != null) {
        var destination = Files.getFileAttributeView(temporary, FileOwnerAttributeView.class);
        if (!owner.getOwner().equals(destination.getOwner())) {
          destination.setOwner(owner.getOwner());
        }
      }
    }
  }

  @FunctionalInterface
  interface SnapshotWriter {
    void write(Path path, String content) throws IOException;
  }

  @FunctionalInterface
  interface AtomicReplacement {
    void replace(Path source, Path target) throws IOException;
  }
}
