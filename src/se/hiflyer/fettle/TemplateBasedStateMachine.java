package se.hiflyer.fettle;

import com.google.common.collect.ImmutableMap;
import se.hiflyer.fettle.util.Multimap;
import se.hiflyer.fettle.util.TransitionMap;

public class TemplateBasedStateMachine<S, E> implements StateMachine<S, E>, StateMachineInternalsInformer<S, E> {
	private final MutableTransitionModel<S, E> template;
	private S currentState;

	public TemplateBasedStateMachine(MutableTransitionModel<S, E> template, S initial) {
		this.template = template;
		currentState = initial;
	}

	@Override
	public S getCurrentState() {
		return currentState;
	}

	@Override
	public boolean fireEvent(E event) {
		return fireEvent(event, Arguments.NO_ARGS);
	}

	@Override
	public boolean fireEvent(E event, Arguments args) {
		Multimap<S, Transition<S, E>> stateTransitions = template.getStateTransitions();
		for (Transition<S, E> transition : stateTransitions.get(currentState)) {
			if (transition.getEvent().equals(event)) {
				if (transition.getCondition().isSatisfied(args)) {
					moveToNewState(transition, event, args);
					return true;
				}
			}
		}
		Transition<S, E> fromAllTransition = template.getFromAllTransitionForEvent(event);
		if (fromAllTransition != null) {
			if (fromAllTransition.getCondition().isSatisfied(args)) {
				moveToNewState(fromAllTransition, event, args);
				return true;
			}
		}
		return false;
	}

	private void moveToNewState(Transition<S, E> transition, E cause, Arguments args) {
		S from = currentState;
		S to = transition.getTo();
		template.runTransitionActions(from, to, cause, args, transition.getTransitionActions());
		currentState = to;
	}

	@Override
	public void forceSetState(S forcedState) {
		Transition<S, E> transition = new Transition<S, E>(currentState, forcedState, BasicConditions.ALWAYS, null);
		moveToNewState(transition, null, Arguments.NO_ARGS);
	}

	@Override
	public TransitionMap<S, E> getStateTransitions() {
		return template.getStateTransitions();
	}

	@Override
	public ImmutableMap<E, Transition<S, E>> getFromAllTransitions() {
		return template.getFromAllTransitions();
	}

	public MutableTransitionModel<S, E> getTemplate() {
		return template;
	}
}
