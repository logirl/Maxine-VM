/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
/*
 * @Harness: java
 * @Runs: 0 = "class java.lang.Object"; 1 = "class java.lang.String";
 * @Runs: 2 = "class jtt.lang.Class_forName02"; 3 = !java.lang.ClassNotFoundException
 * @Runs: 4 = null
 */
package jtt.lang;

public final class Class_forName02 {
    private Class_forName02() {
    }

    public static String test(int i) throws ClassNotFoundException {
        String clname = null;
        Class cl = null;
        if (i == 0) {
            clname = "java.lang.Object";
            cl = Object.class;
        } else if (i == 1) {
            clname = "java.lang.String";
            cl = String.class;
        } else if (i == 2) {
            clname = "jtt.lang.Class_forName02";
            cl = Class_forName02.class;
        } else if (i == 3) {
            clname = "xyzz.zyxy.XYXY";
            cl = Class_forName02.class;
        }
        if (clname != null) {
            return Class.forName(clname, false, cl.getClassLoader()).toString();
        }
        return null;
    }
}