/*
 * Copyright (c) 2007 Sun Microsystems, Inc.  All rights reserved.
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

package com.sun.max.vm.cps.b.c.d;

import com.sun.max.collect.*;
import com.sun.max.vm.*;
import com.sun.max.vm.cps.b.c.*;
import com.sun.max.vm.cps.cir.dir.*;
import com.sun.max.vm.cps.dir.*;
import com.sun.max.vm.cps.ir.*;

/**
 * @author Bernd Mathiske
 */
public class BcdCompiler extends BcCompiler implements DirGeneratorScheme {

    private final CirToDirTranslator cirToDirTranslator;

    public BcdCompiler(VMConfiguration vmConfiguration) {
        super(vmConfiguration);
        cirToDirTranslator = new CirToDirTranslator(this);
    }

    public DirGenerator dirGenerator() {
        return cirToDirTranslator;
    }

    @Override
    public IrGenerator irGenerator() {
        return dirGenerator();
    }

    @Override
    public Sequence<IrGenerator> irGenerators() {
        return Sequence.Static.appended(super.irGenerators(), cirToDirTranslator);
    }
}