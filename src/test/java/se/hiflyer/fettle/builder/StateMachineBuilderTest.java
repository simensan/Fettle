package se.hiflyer.fettle.builder;

import com.googlecode.gentyref.TypeToken;
import org.junit.Test;
import se.hiflyer.fettle.Action;
import se.hiflyer.fettle.Condition;
import se.hiflyer.fettle.StateMachine;
import se.hiflyer.fettle.StateMachineTemplate;
import se.hiflyer.fettle.States;
import se.hiflyer.fettle.export.DotExporter;
import se.hiflyer.fettle.impl.AbstractTransitionModel;
import se.hiflyer.fettle.util.GuavaReplacement;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static se.mockachino.Mockachino.mock;
import static se.mockachino.Mockachino.verifyNever;
import static se.mockachino.Mockachino.verifyOnce;
import static se.mockachino.matchers.Matchers.any;

public class StateMachineBuilderTest {

	public static final TypeToken<Action<States,String, Void>> ACTION_TYPE_TOKEN = new TypeToken<Action<States, String, Void>>() {
	};

	@Test
	public void testBuilder() {
		StateMachineBuilder<States, String, Void> builder = StateMachineBuilder.create(States.class, String.class);

		builder.transition().from(States.INITIAL).to(States.ONE).on("hej");
		builder.transition().from(States.ONE).to(States.TWO).on("hopp");

		StateMachineTemplate<States, String, Void> stateMachineTemplate = builder.buildTransitionModel();
		StateMachine<States, String, Void> machine = stateMachineTemplate.newStateMachine(States.INITIAL);

		assertEquals(States.INITIAL, machine.getCurrentState());

		machine.fireEvent("hej");

		assertEquals(States.ONE, machine.getCurrentState());

		machine.fireEvent("hej");

		assertEquals(States.ONE, machine.getCurrentState());

		machine.fireEvent("hopp");


		assertEquals(States.TWO, machine.getCurrentState());
		DotExporter<States, String, Void> exporter = new DotExporter<States, String, Void>((AbstractTransitionModel<States, String, Void>) stateMachineTemplate, "test");
		exporter.asDot(System.out, true);
	}

	@Test
	public void fromAllTransition() {

		StateMachineBuilder<States, String, Void> builder = StateMachineBuilder.create(States.class, String.class);


		builder.transition().from(States.INITIAL).to(States.ONE).on("hej");
		builder.transition().from(States.ONE).to(States.TWO).on("hopp");
		builder.transition().fromAll().to(States.INITIAL).on("back");

		StateMachine<States, String, Void> machine = builder.build(States.INITIAL);

		machine.fireEvent("hej");
		machine.fireEvent("hopp");
		machine.fireEvent("back");


		assertEquals(States.INITIAL, machine.getCurrentState());

	}

	@Test
	public void entryExitActions() {

		StateMachineBuilder<States, String, Void> builder = StateMachineBuilder.create(States.class, String.class);

		builder.transition().from(States.INITIAL).to(States.ONE).on("");

		builder.transition().from(States.ONE).to(States.TWO).on("");
		Action<States, String, Void> entryAction = mock(ACTION_TYPE_TOKEN);
		builder.onEntry(States.ONE).perform(entryAction);

		Action<States, String, Void> exitAction1 = mock(ACTION_TYPE_TOKEN);
		Action<States, String, Void> exitAction2 = mock(ACTION_TYPE_TOKEN);
		List<Action<States, String, Void>> exitActions = GuavaReplacement.newArrayList();
		exitActions.add(exitAction1);
		exitActions.add(exitAction2);
		builder.onExit(States.ONE).perform(exitActions);

		StateMachine<States, String, Void> machine = builder.build(States.INITIAL);

		machine.fireEvent("foo");

		verifyNever().on(entryAction).onTransition(any(States.class), any(States.class), any(String.class), any(Void.class), machine);
		verifyNever().on(exitAction1).onTransition(any(States.class), any(States.class), any(String.class), any(Void.class), machine);
		verifyNever().on(exitAction2).onTransition(any(States.class), any(States.class), any(String.class), any(Void.class), machine);

		machine.fireEvent("");

		verifyOnce().on(entryAction).onTransition(any(States.class), any(States.class), any(String.class), any(Void.class), machine);
		verifyNever().on(exitAction1).onTransition(any(States.class), any(States.class), any(String.class), any(Void.class), machine);
		verifyNever().on(exitAction2).onTransition(any(States.class), any(States.class), any(String.class), any(Void.class), machine);

		machine.fireEvent("");

		verifyOnce().on(entryAction).onTransition(any(States.class), any(States.class), any(String.class), any(Void.class), machine);
		verifyOnce().on(exitAction1).onTransition(any(States.class), any(States.class), any(String.class), any(Void.class), machine);
		verifyOnce().on(exitAction2).onTransition(any(States.class), any(States.class), any(String.class), any(Void.class), machine);
	}

	@Test
	public void when() throws Exception {
		StateMachineBuilder<States, String, Void> builder = StateMachineBuilder.create(States.class, String.class);

		ConditionImpl condition = new ConditionImpl();
		builder.transition().from(States.INITIAL).to(States.ONE).on("").when(condition);

		StateMachine<States, String, Void> machine = builder.build(States.INITIAL);

		machine.fireEvent("");
		assertEquals(States.INITIAL, machine.getCurrentState());

		condition.pass = true;
		machine.fireEvent("");
		assertEquals(States.ONE, machine.getCurrentState());
	}

	@Test
	public void missingOn() throws Exception {
		StateMachineBuilder<States, String, Void> builder = StateMachineBuilder.create(States.class, String.class);
		builder.transition().from(States.INITIAL).to(States.ONE);
		try {
			StateMachine<States, String, Void> stateMachine = builder.build(States.INITIAL);
			fail("Should not be allowed to build a state machine with transition on null");
		} catch (IllegalStateException e) {
			assertTrue(e.getMessage().contains(States.INITIAL.toString()));
			assertTrue(e.getMessage().contains(States.ONE.toString()));
		}
	}

	@Test
	public void missingOnFromAll() throws Exception {
		StateMachineBuilder<States, String, Void> builder = StateMachineBuilder.create(States.class, String.class);
		builder.transition().fromAll().to(States.ONE);
		try {
			StateMachine<States, String, Void> stateMachine = builder.build(States.INITIAL);
			fail("Should not be allowed to build a state machine with transition on null");
		} catch (IllegalStateException e) {
			assertTrue(e.getMessage().contains(States.ONE.toString()));
		}
	}


	private class ConditionImpl implements Condition<Void> {
		boolean pass = false;

		@Override
		public boolean isSatisfied(Void ignored) {
			return pass;
		}
	}

	@Test
	public void perform() throws Exception {
		StateMachineBuilder<States, String, Void> builder = StateMachineBuilder.create(States.class, String.class);
		Action<States, String, Void> action1 = mock(ACTION_TYPE_TOKEN);
		Action<States, String, Void> action2 = mock(ACTION_TYPE_TOKEN);

		List<Action<States, String, Void>> actions = GuavaReplacement.newArrayList();
		actions.add(action1);
		actions.add(action2);

		builder.transition().from(States.INITIAL).to(States.ONE).on("a").perform(actions);

		StateMachine<States, String, Void> machine = builder.build(States.INITIAL);

		verifyNever().on(action1).onTransition(any(States.class), any(States.class), any(String.class), any(Void.class), machine);
		verifyNever().on(action2).onTransition(any(States.class), any(States.class), any(String.class), any(Void.class), machine);
		machine.fireEvent("");

		verifyNever().on(action1).onTransition(any(States.class), any(States.class), any(String.class), any(Void.class), machine);
		verifyNever().on(action2).onTransition(any(States.class), any(States.class), any(String.class), any(Void.class), machine);

		machine.fireEvent("a");

		verifyOnce().on(action1).onTransition(States.INITIAL, States.ONE, "a", any(Void.class), machine);
		verifyOnce().on(action2).onTransition(States.INITIAL, States.ONE, "a", any(Void.class), machine);
	}

	@Test
	public void nullInitialState() throws Exception {
		StateMachineBuilder<States, String, Void> builder = StateMachineBuilder.create(States.class, String.class);
		try {
			builder.build(null);
			fail("Null should not be permitted");
		} catch (IllegalArgumentException e) {

		}
	}
}
