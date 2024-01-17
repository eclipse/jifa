#!/usr/bin/perl
# Copyright (c) 2023, 2024 Contributors to the Eclipse Foundation
#
# See the NOTICE file(s) distributed with this work for additional
# information regarding copyright ownership.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License 2.0 which is available at
# http://www.eclipse.org/legal/epl-2.0
#
# SPDX-License-Identifier: EPL-2.0

use strict;
use utf8;

use File::Copy;

require File::Find;

binmode(STDOUT, ":encoding(utf8)");
binmode(STDIN, ":encoding(utf8)");
binmode(STDERR, ":encoding(utf8)");

my $tmp_path = '/tmp/.jifa_licence_tmp';

my $license_1 =
'/********************************************************************************
 * Copyright (c) 2024 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/';

my $license_2 =
'# Copyright (c) 2024 Contributors to the Eclipse Foundation
#
# See the NOTICE file(s) distributed with this work for additional
# information regarding copyright ownership.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License 2.0 which is available at
# http://www.eclipse.org/legal/epl-2.0
#
# SPDX-License-Identifier: EPL-2.0';

my $license_3 =
'<!--
    Copyright (c) 2024 Contributors to the Eclipse Foundation

    See the NOTICE file(s) distributed with this work for additional
    information regarding copyright ownership.

    This program and the accompanying materials are made available under the
    terms of the Eclipse Public License 2.0 which is available at
    http://www.eclipse.org/legal/epl-2.0

    SPDX-License-Identifier: EPL-2.0
 -->';

sub prepend_licence {
  my $name = shift;
  my $license = shift;
  my $target_line = shift;
  my $line = 1;

  open(FH, "<$name") or die "Cannot open $name$!";

  open(FH_TMP, ">$tmp_path") or die "Cannot open $tmp_path: $!";

  while(<FH>) {
    if ($_ =~ /SPDX-License-Identifier: EPL-2.0/) {
      close(FH_TMP);
      close(FH);

      unlink($tmp_path);
      return;
    }

    if ($line == $target_line) {
      print FH_TMP "$license\n";
    }
    $line++;
    print FH_TMP $_;
  }

  close(FH_TMP);
  close(FH);
  unlink($name);
  move($tmp_path, $name);
}

sub callback {
  unless (-d) {
    my $dir = $File::Find::dir;
    my $path = $File::Find::name;
    my $name = $_;
    if ($dir =~ /\.git/ ||
        $dir =~ /node_modules/ ||
        $dir =~ /.build/ ||
        $dir =~ /.gradle/ ||
        $dir =~ /.vscode/ ||
        $dir =~ /.idea/ ||
        $dir =~ /resources/ ||
        $name eq '.gitignore' ||
        $name eq 'auto-imports.d.ts' ||
        $name eq 'components.d.ts') {
      return;
    }

    if ($name =~ /\.java\z/ || $name =~ /\.gradle\z/ || $name =~ /\.ts\z/ || $name =~ /\.ts\z/) {
      prepend_licence($name, $license_1, 1);
    } elsif ($name =~ /\.xml\z/) {
      # skip first line
      prepend_licence($name, $license_3, 2);
    } elsif ($name =~ /\.sh\z/) {
      # skip first line
      prepend_licence($name, $license_2, 2);
    } elsif ($name =~ /\.yml\z/ || $name =~ /.yaml\z/) {
      prepend_licence($name, $license_2, 1);
    } elsif ($name =~ /\.vue\z/) {
      prepend_licence($name, $license_3, 1);
    } else {
      print "Skip $path\n"
    }
  };
}

my @recursiveFolder = qw(.);

File::Find::find(\&callback, @recursiveFolder);
