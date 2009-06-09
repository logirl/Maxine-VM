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
package com.sun.c1x.ir;

import com.sun.c1x.util.InstructionVisitor;
import com.sun.c1x.value.ValueType;
import com.sun.c1x.ci.CiType;

/**
 * The <code>Local</code> instruction is a placeholder for an incoming argument
 * to a function call.
 *
 * @author Ben L. Titzer
 */
public class Local extends Instruction {

    private final int _javaIndex;
    private CiType _declaredType;

    public Local(ValueType type, int javaIndex) {
        super(type);
        _javaIndex = javaIndex;
    }

    /**
     * Gets the index of this local.
     * @return the index
     */
    public int javaIndex() {
        return _javaIndex;
    }

    /**
     * Sets the declared type of this local, e.g. derived from the signature of the method.
     * @param declaredType the declared type of the local variable
     */
    public void setDeclaredType(CiType declaredType) {
        _declaredType = declaredType;
    }

    /**
     * Computes the declared type of the result of this instruction, if possible.
     * @return the declared type of the result of this instruction, if it is known; <code>null</code> otherwise
     */
    public CiType declaredType() {
        return _declaredType;
    }

    /**
     * Implements this instruction's half of the visitor pattern.
     * @param v the visitor to dispatch to
     */
    public void accept(InstructionVisitor v) {
        v.visitLocal(this);
    }
}