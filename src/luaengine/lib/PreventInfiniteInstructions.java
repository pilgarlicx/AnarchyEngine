package luaengine.lib;

import java.util.HashMap;

import org.luaj.vm2.LuaClosure;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.DebugLib;

import luaengine.type.ScriptData;

public class PreventInfiniteInstructions extends DebugLib {
	private HashMap<Thread,LuaThreadInstructions> instructions;
	
	public PreventInfiniteInstructions() {
		this.instructions = new HashMap<Thread,LuaThreadInstructions>();
	}
	
	private void check() {
		Thread c = Thread.currentThread();
		if ( !instructions.containsKey(c) ) {
			instructions.put(c, new LuaThreadInstructions());
		}
	}

	@Override
	public void onCall(LuaClosure c, Varargs varargs, LuaValue[] stack) {
		check();
		instructions.get(Thread.currentThread()).instructions = 0;
		
		super.onCall(c, varargs, stack);
	}
	
	@Override
	public void onInstruction(int pc, Varargs v, int top) {
		check();
		LuaThreadInstructions inst = instructions.get(Thread.currentThread());
		
		// If this thread was interrupted (see below conditional) then throw exception to stop code from running.
		if (inst.interrupted) {
			System.out.println("Throwing error on thread: " + Thread.currentThread());
			instructions.remove(Thread.currentThread());
			throw new ScriptInterruptException();
		}
		
		// Check if the thread was interrupted or we have too many instructions looping (100 million).
		if ( inst.instructions > 1.0e8 || ScriptData.isInterrupted(Thread.currentThread())) {
			System.out.println("Cancelling the current closure");
			inst.interrupted = true;
		}
		
		// Increment instructions by 1
		inst.instructions++;
		
		super.onInstruction(pc, v, top);
	}
	
	static class LuaThreadInstructions {
		int instructions;
		boolean interrupted;
	}
}