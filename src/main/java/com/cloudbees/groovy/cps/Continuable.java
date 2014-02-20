package com.cloudbees.groovy.cps;

import com.cloudbees.groovy.cps.impl.CpsCallableInvocation;
import com.cloudbees.groovy.cps.impl.CpsFunction;
import com.cloudbees.groovy.cps.impl.FunctionCallEnv;
import com.cloudbees.groovy.cps.impl.YieldBlock;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Mutable representation of the program. This is the primary API of the groovy-cps library to the outside.
 *
 * @author Kohsuke Kawaguchi
 */
public class Continuable implements Serializable {
    /**
     * Represents the remainder of the program to execute.
     */
    private Continuation program;

    public Continuable(Continuation program) {
        this.program = program;
    }

    public Continuable(Next program) {
        this(program.asContinuation());
    }

    /**
     * Creates a {@link Continuable} that executes the block of code.
     */
    public Continuable(Block block) {
        this(new Next(block,
                new FunctionCallEnv(null,null,Continuation.HALT),
                Continuation.HALT).asContinuation());
    }

    /**
     * Creates a shallow copy of {@link Continuable}. The copy shares
     * all the local variables of the original {@link Continuable}, and
     * point to the exact same point of the program.
     */
    public Continuable fork() {
        return new Continuable(program);
    }

    /**
     * Runs this program until it suspends the next time.
     */
    public Object run(Object arg) {
        Next n = program.receive(arg).run();
        // when yielding, we resume from the continuation so that we can pass in the value.
        // see Next#yield
        program = n.k;
        return n.yieldedValue();
    }

    /**
     * Checks if this {@link Continuable} is pointing at the end of the program which cannot
     * be resumed.
     *
     * If this method returns false, it is illegal to call {@link #run(Object)}
     */
    public boolean isResumable() {
        return program!=Continuation.HALT;
    }

    /**
     * Called from within CPS transformed program to suspends the execution.
     *
     * <p>
     * When this method is called, the control goes back to
     * the caller of {@link #run(Object)}, which returns with the argument given to this method.
     *
     * <p>
     * When the continuable is resumed via {@link #run(Object)} later, the argument to the run method
     * will become the return value from this method to the CPS-transformed program.
     */
    public static Object suspend(final Object v) {
        throw new CpsCallableInvocation(new CpsFunction(Arrays.asList("v"), new YieldBlock(v)),null,v);
    }

    private static final long serialVersionUID = 1L;
}