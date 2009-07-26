/*
 * Copyright (c) 2009 Sun Microsystems, Inc.  All rights reserved.
 *
 * Sun Microsystems, Inc. has intellectual property rights relating to technology embodied in the product
 * that is described in this document. In particular, and without limitation, these intellectual property
 * rights may include one or more of the U.S. patents listed at http://www.sun.com/patents and one or
 * more additional patents or pending patent applications in the U.S. and in other countries.
 *
 * U.S. Government Rights - Commercial software. Government users are subject to the Sun
 * Microsystems, Inc. standard license agreement and applicable provisions of the FAR and its
 * supplements.
 *
 * Use is subject to license terms. Sun, Sun Microsystems, the Sun logo, Java and Solaris are trademarks or
 * registered trademarks of Sun Microsystems, Inc. in the U.S. and other countries. All SPARC trademarks
 * are used under license and are trademarks or registered trademarks of SPARC International, Inc. in the
 * U.S. and other countries.
 *
 * UNIX is a registered trademark in the U.S. and other countries, exclusively licensed through X/Open
 * Company, Ltd.
 */
package test.optimize;

/*
 * Tests value numbering of instanceof operations.
 * @Harness: java
 * @Runs: 0=true; 1=true; 2=false
 */
public class VN_InstanceOf02 {
    private static boolean cond = true;

    static final Object _object = new VN_InstanceOf02();

    public static boolean test(int arg) {
        if (arg == 0) {
            return test1();
        }
        if (arg == 1) {
            return test2();
        }
        if (arg == 2) {
            return test3();
        }
        // do nothing
        return false;
    }

    private static boolean test1() {
        boolean a = _object instanceof VN_InstanceOf02;
        if (cond) {
            boolean b = _object instanceof VN_InstanceOf02;
            return a | b;
        }
        return false;
    }

    private static boolean test2() {
        Object obj = new VN_InstanceOf02();
        boolean a = obj instanceof VN_InstanceOf02;
        if (cond) {
            boolean b = obj instanceof VN_InstanceOf02;
            return a | b;
        }
        return false;
    }

    private static boolean test3() {
        boolean a = null instanceof VN_InstanceOf02;
        if (cond) {
            boolean b = null instanceof VN_InstanceOf02;
            return a | b;
        }
        return false;
    }
}