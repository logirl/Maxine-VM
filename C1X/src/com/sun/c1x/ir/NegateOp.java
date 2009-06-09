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

import com.sun.c1x.util.InstructionClosure;
import com.sun.c1x.util.InstructionVisitor;

/**
 * The <code>NegateOp</code> instruction negates its operand.
 *
 * @author Ben L. Titzer
 */
public class NegateOp extends Instruction {

    Instruction _x;

    /**
     * Creates new NegateOp instance.
     * @param x the instruction producing the value that is input to this instruction
     */
    public NegateOp(Instruction x) {
        super(x.type().base());
    }

    /**
     * Gets the instruction producing input to this instruction.
     * @return the instruction that produces this instruction's input
     */
    public Instruction x() {
        return _x;
    }

    /**
     * Iterates over the input values to this instruction.
     * @param closure the closure to apply to each value
     */
    public void inputValuesDo(InstructionClosure closure) {
        _x = closure.apply(_x);
    }

    /**
     * Implements this instruction's half of the visitor pattern.
     * @param v the visitor to accept
     */
    public void accept(InstructionVisitor v) {
        v.visitNegateOp(this);
    }
}