package se.hiflyer.fettle.util;

import com.google.common.base.Predicate;
import se.hiflyer.fettle.Condition;

public class PredicateCondition<C> implements Condition<C> {

	private final Predicate<? super C> predicate;

	public PredicateCondition(Predicate<? super C> predicate) {
		this.predicate = predicate;
	}

	@Override
	public boolean isSatisfied(C context) {
		return predicate.apply(context);
	}
}
