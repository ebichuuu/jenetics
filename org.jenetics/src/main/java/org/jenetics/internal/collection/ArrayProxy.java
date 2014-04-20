/*
 * Java Genetic Algorithm Library (@__identifier__@).
 * Copyright (c) @__year__@ Franz Wilhelmstötter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Author:
 *    Franz Wilhelmstötter (franz.wilhelmstoetter@gmx.at)
 */
package org.jenetics.internal.collection;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

import java.io.Serializable;
import java.util.function.Function;
import java.util.function.IntFunction;

import org.jenetics.util.Copyable;

/**
 * Abstraction for an ordered and bounded sequence of elements.
 *
 * @author <a href="mailto:franz.wilhelmstoetter@gmx.at">Franz Wilhelmstötter</a>
 * @since 1.4
 * @version 3.0 &mdash; <em>$Date: 2014-04-21 $</em>
 */
public abstract class ArrayProxy<T, A, P extends ArrayProxy<T, A, P>>
	implements
		Copyable<P>,
		Serializable
{
	private static final long serialVersionUID = 1L;

	public A _array;
	private boolean _sealed = false;

	protected final int _start;
	protected final int _end;
	protected final int _length;

	private final ArrayProxyFactory<A, P> _factory;
	private final ArrayCopier<A> _copier;

	protected ArrayProxy(
		final A array,
		final int start,
		final int end,
		final ArrayProxyFactory<A, P> factory,
		final ArrayCopier<A> copier
	) {
		if (start < 0 || end < 0 || end < start) {
			throw new IllegalArgumentException(format(
				"Invalid indexes [%d, %d)", start, end
			));
		}

		_array = requireNonNull(array);
		_start = start;
		_end = end;
		_length = _end - _start;
		_factory = requireNonNull(factory);
		_copier = requireNonNull(copier);
	}

	/**
	 * Return the <i>array</i> element at the specified, absolute position in
	 * the {@code ArrayProxy}. The array boundaries are not checked.
	 *
	 * @param index absolute index of the element to return
	 * @return the <i>array</i> element at the specified absolute position
	 */
	public abstract T __get(final int index);

	/**
	 * Set the <i>array</i> element at the specified absolute position in the
	 * {@code ArrayProxy}. The array boundaries are not checked.
	 *
	 * @param index absolute index of the <i>array</i> element
	 */
	public abstract void __set(final int index, final T value);

	/**
	 * Return the <i>array</i> element at the specified position in the
	 * {@code ArrayProxy}. The array boundaries are not checked.
	 *
	 * @param index index of the element to return
	 * @return the <i>array</i> element at the specified position
	 */
	public T uncheckedGet(final int index) {
		return __get(index + _start);
	}

	/**
	 * Set the <i>array</i> element at the specified position in the
	 * {@code ArrayProxy}. The array boundaries are not checked.
	 *
	 * @param index index of the <i>array</i> element
	 */
	public void uncheckedSet(final int index, final T value) {
		__set(index + _start, value);
	}

	/**
	 * Return the <i>array</i> element at the specified position in the
	 * {@code ArrayProxy}.
	 *
	 * @param index index of the element to return
	 * @return the <i>array</i> element at the specified position
	 * @throws IndexOutOfBoundsException if the index it out of range
	 *         (index < 0 || index >= _length).
	 */
	public T get(final int index) {
		checkIndex(index);
		return __get(index + _start);
	}

	/**
	 * Set the <i>array</i> element at the specified position in the
	 * {@code ArrayProxy}
	 *
	 * @param index the index of the element to set
	 * @param value the <i>array</i> element
	 * @throws IndexOutOfBoundsException if the index it out of range
	 *         (index < 0 || index >= _length).
	 */
	public void set(final int index, final T value) {
		checkIndex(index);
		__set(index + _start, value);
	}

	/**
	 * Return a new sub {@code ArrayProxy} object with the given start and end
	 * indexes. The underlying array storage is not copied. With the returned
	 * sub-array proxy it is possible to <i>write through</i> the original
	 * array.
	 *
	 * @param from the start index of the new sub {@code ArrayProxy} object,
	 *        inclusively.
	 * @param until the end index of the new sub {@code ArrayProxy} object,
	 *        exclusively.
	 * @return a new array proxy (view) with the given start and end index.
	 * @throws IndexOutOfBoundsException if the given indexes are out of bounds.
	 */
	public P slice(final int from, final int until) {
		return _factory.create(_array, from + _start, until + _start);
	}

	/**
	 * Return a new sub {@code ArrayProxy} object with the given start index.
	 * The underlying array storage is not copied. With the returned sub-array
	 * proxy it is possible to <i>write through</i> the original array.
	 *
	 * @param from the start index of the new sub {@code ArrayProxy} object,
	 *        inclusively.
	 * @return a new array proxy (view) with the given start index.
	 * @throws IndexOutOfBoundsException if the given indexes are out of bounds.
	 */
	public P slice(final int from) {
		return slice(from, _length);
	}

	/**
	 * Swap a given range with a range of the same size with another array.
	 * Implementations of this class should replace this with a optimized
	 * version, depending on the underlying data structure.
	 *
	 * <pre>
	 *            from                until
	 *              |                   |
	 * this:  +---+---+---+---+---+---+---+---+---+---+---+---+
	 *              +---------------+
	 *                          +---------------+
	 * other: +---+---+---+---+---+---+---+---+---+---+---+---+
	 *                          |
	 *                      otherFrom
	 * </pre>
	 *
	 * @param start the start index of {@code this} range, inclusively.
	 * @param end the end index of {@code this} range, exclusively.
	 * @param other the other array to swap the elements with.
	 * @param otherStart the start index of the {@code other} array.
	 * @throws IndexOutOfBoundsException if {@code start > end} or
	 *         if {@code from < 0 || until >= this.length() || otherFrom < 0 ||
	 *         otherFrom + (until - from) >= other.length()}
	 */
	public void swap(
		final int start,
		final int end,
		final ArrayProxy<T, ?, ?> other,
		final int otherStart
	) {
		checkIndex(start, end);
		other.checkIndex(otherStart, otherStart + (end - start));
		cloneIfSealed();
		other.cloneIfSealed();

		for (int i = (end - start); --i >= 0;) {
			final T temp = uncheckedGet(i + start);
			uncheckedSet(i + start, other.uncheckedGet(otherStart + i));
			other.uncheckedSet(otherStart + i, temp);
		}
	}

	public final <B> ObjectArrayProxy<B> map(
		final Function<? super T, ? extends B> mapper
	) {
		return map(mapper, ObjectArrayProxy<B>::new);
	}

	public final <B, P extends ArrayProxy<B, ?, ?>> P map(
		final Function<? super T, ? extends B> mapper,
		final IntFunction<P> builder
	) {
		final P result = builder.apply(_length);
		assert (result._length == _length);

		for (int i = 0; i < _length; ++i) {
			result.__set(i, mapper.apply(uncheckedGet(i)));
		}
		return result;
	}

	/**
	 * Clone the underlying data structure of this {@code ArrayProxy} if it is
	 * sealed.
	 */
	public final void cloneIfSealed() {
		if (_sealed) {
			_array = _copier.copy(_array, 0, _end);
			_sealed = false;
		}
	}


	/**
	 * Set the seal flag for this {@code ArrayProxy} instance and return a new
	 * {@code ArrayProxy} object with an not set <i>seal</i> flag but with the
	 * same underlying data structure.
	 *
	 * @return a new {@code ArrayProxy} instance; for command chaining.
	 */
	public final P seal() {
		_sealed = true;
		return _factory.create(_array, _start, _end);
	}

	@Override
	public P copy() {
		return _factory.create(_copier.copy(_array, _start, _end), 0, _end - _start);
	}

	/**
	 * Checks the given index.
	 *
	 * @param start the index to check.
	 * @throws java.lang.ArrayIndexOutOfBoundsException if the given index is
	 *         not in the valid range.
	 */
	protected final void checkIndex(final int start) {
		if (start < 0 || start >= _length) {
			throw new ArrayIndexOutOfBoundsException(format(
				"Index %s is out of bounds [0, %s)", start, _length
			));
		}
	}

	/**
	 * Check the given {@code from} and {@code until} indices.
	 *
	 * @param start the start index, inclusively.
	 * @param end the end index, exclusively.
	 * @throws java.lang.ArrayIndexOutOfBoundsException if the given index is
	 *         not in the valid range.
	 */
	protected final void checkIndex(final int start, final int end) {
		if (start > end) {
			throw new ArrayIndexOutOfBoundsException(format(
				"fromIndex(%d) > toIndex(%d)", start, end
			));
		}
		if (start < 0 || end > _length) {
			throw new ArrayIndexOutOfBoundsException(format(
				"Invalid index range: [%d, %s)", start, end
			));
		}
	}

}