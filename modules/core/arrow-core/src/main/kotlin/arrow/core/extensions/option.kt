@file:Suppress("UnusedImports")

package arrow.core.extensions

import arrow.Kind
import arrow.core.Either
import arrow.core.Eval
import arrow.core.ForOption
import arrow.core.None
import arrow.core.Option
import arrow.core.OptionOf
import arrow.core.Some
import arrow.core.Tuple2
import arrow.extension
import arrow.core.fix
import arrow.core.extensions.traverse as optionTraverse
import arrow.core.extensions.option.monad.map
import arrow.core.extensions.option.monad.monad
import arrow.core.identity
import arrow.core.orElse
import arrow.typeclasses.Applicative
import arrow.typeclasses.ApplicativeError
import arrow.typeclasses.Apply
import arrow.typeclasses.Eq
import arrow.typeclasses.Foldable
import arrow.typeclasses.Functor
import arrow.typeclasses.Hash
import arrow.typeclasses.Monad
import arrow.typeclasses.MonadSyntax
import arrow.typeclasses.MonadError
import arrow.typeclasses.MonadFx
import arrow.typeclasses.Monoid
import arrow.typeclasses.MonoidK
import arrow.typeclasses.Monoidal
import arrow.typeclasses.Selective
import arrow.typeclasses.Semigroup
import arrow.typeclasses.SemigroupK
import arrow.typeclasses.Semigroupal
import arrow.typeclasses.Semiring
import arrow.typeclasses.Show
import arrow.typeclasses.Traverse
import arrow.core.select as optionSelect

@extension
interface OptionSemigroup<A> : Semigroup<Option<A>> {

  fun SG(): Semigroup<A>

  override fun Option<A>.combine(b: Option<A>): Option<A> =
    when (this) {
      is Some<A> -> when (b) {
        is Some<A> -> Some(SG().run { t.combine(b.t) })
        None -> this
      }
      None -> b
    }
}

@extension
interface OptionSemigroupal : Semigroupal<ForOption> {
  override fun <A, B> Kind<ForOption, A>.product(fb: Kind<ForOption, B>): Kind<ForOption, Tuple2<A, B>> =
    fb.fix().ap(this.map { a: A -> { b: B -> Tuple2(a, b) } })
}

@extension
interface OptionMonoidal : Monoidal<ForOption>, OptionSemigroupal {
  override fun <A> identity(): Kind<ForOption, A> = None
}

@extension
interface OptionMonoid<A> : Monoid<Option<A>>, OptionSemigroup<A> {
  override fun SG(): Semigroup<A>
  override fun empty(): Option<A> = None
}

@extension
interface OptionSemiring<A> : Semiring<Option<A>> {

  fun SG(): Semiring<A>
  override fun zero(): Option<A> = None
  override fun one(): Option<A> = None

  override fun Option<A>.combine(b: Option<A>): Option<A> =
    when (this) {
      is Some<A> -> when (b) {
        is Some<A> -> Some(SG().run { t.combine(b.t) })
        None -> this
      }
      None -> b
    }

  override fun Option<A>.combineMultiplicate(b: Option<A>): Option<A> =
    when (this) {
      is Some<A> -> when (b) {
        is Some<A> -> Some(SG().run { t.combineMultiplicate(b.t) })
        None -> this
      }
      None -> b
    }
}

@extension
interface OptionApplicativeError : ApplicativeError<ForOption, Unit>, OptionApplicative {
  override fun <A> raiseError(e: Unit): Option<A> =
    None

  override fun <A> OptionOf<A>.handleErrorWith(f: (Unit) -> OptionOf<A>): Option<A> =
    fix().orElse { f(Unit).fix() }
}

@extension
interface OptionMonadError : MonadError<ForOption, Unit>, OptionMonad {
  override fun <A> raiseError(e: Unit): OptionOf<A> =
    None

  override fun <A> OptionOf<A>.handleErrorWith(f: (Unit) -> OptionOf<A>): Option<A> =
    fix().orElse { f(Unit).fix() }
}

@extension
interface OptionEq<A> : Eq<Option<A>> {

  fun EQ(): Eq<A>

  override fun Option<A>.eqv(b: Option<A>): Boolean = when (this) {
    is Some -> when (b) {
      None -> false
      is Some -> EQ().run { t.eqv(b.t) }
    }
    None -> when (b) {
      None -> true
      is Some -> false
    }
  }
}

@extension
interface OptionShow<A> : Show<Option<A>> {
  override fun Option<A>.show(): String =
    toString()
}

@extension
interface OptionFunctor : Functor<ForOption> {
  override fun <A, B> OptionOf<A>.map(f: (A) -> B): Option<B> =
    fix().map(f)
}

@extension
interface OptionApply : Apply<ForOption> {
  override fun <A, B> OptionOf<A>.ap(ff: OptionOf<(A) -> B>): Option<B> =
    fix().ap(ff)

  override fun <A, B> OptionOf<A>.map(f: (A) -> B): Option<B> =
    fix().map(f)
}

@extension
interface OptionApplicative : Applicative<ForOption> {
  override fun <A, B> OptionOf<A>.ap(ff: OptionOf<(A) -> B>): Option<B> =
    fix().ap(ff)

  override fun <A, B> OptionOf<A>.map(f: (A) -> B): Option<B> =
    fix().map(f)

  override fun <A> just(a: A): Option<A> =
    Option.just(a)
}

@extension
interface OptionSelective : Selective<ForOption>, OptionApplicative {
  override fun <A, B> OptionOf<Either<A, B>>.select(f: OptionOf<(A) -> B>): Option<B> =
    fix().optionSelect(f)
}

@extension
interface OptionMonad : Monad<ForOption> {
  override fun <A, B> OptionOf<A>.ap(ff: OptionOf<(A) -> B>): Option<B> =
    fix().ap(ff)

  override fun <A, B> OptionOf<A>.flatMap(f: (A) -> OptionOf<B>): Option<B> =
    fix().flatMap(f)

  override fun <A, B> tailRecM(a: A, f: (A) -> OptionOf<Either<A, B>>): Option<B> =
    Option.tailRecM(a, f)

  override fun <A, B> OptionOf<A>.map(f: (A) -> B): Option<B> =
    fix().map(f)

  override fun <A> just(a: A): Option<A> =
    Option.just(a)

  override fun <A, B> OptionOf<Either<A, B>>.select(f: OptionOf<(A) -> B>): OptionOf<B> =
    fix().optionSelect(f)

  override val fx: MonadFx<ForOption>
    get() = OptionFxMonad
}

internal object OptionFxMonad : MonadFx<ForOption> {
  override val M: Monad<ForOption> = Option.monad()
  override fun <A> monad(c: suspend MonadSyntax<ForOption>.() -> A): Option<A> =
    super.monad(c).fix()
}

@extension
interface OptionFoldable : Foldable<ForOption> {
  override fun <A> OptionOf<A>.exists(p: (A) -> Boolean): Boolean =
    fix().exists(p)

  override fun <A, B> OptionOf<A>.foldLeft(b: B, f: (B, A) -> B): B =
    fix().foldLeft(b, f)

  override fun <A, B> OptionOf<A>.foldRight(lb: Eval<B>, f: (A, Eval<B>) -> Eval<B>): Eval<B> =
    fix().foldRight(lb, f)

  override fun <A> OptionOf<A>.forAll(p: (A) -> Boolean): Boolean =
    fix().forall(p)

  override fun <A> OptionOf<A>.isEmpty(): Boolean =
    fix().isEmpty()

  override fun <A> OptionOf<A>.nonEmpty(): Boolean =
    fix().nonEmpty()
}

@extension
interface OptionSemigroupK : SemigroupK<ForOption> {
  override fun <A> OptionOf<A>.combineK(y: OptionOf<A>): Option<A> =
    orElse { y.fix() }
}

@extension
interface OptionMonoidK : MonoidK<ForOption> {
  override fun <A> empty(): Option<A> =
    Option.empty()

  override fun <A> OptionOf<A>.combineK(y: OptionOf<A>): Option<A> =
    orElse { y.fix() }
}

fun <A, G, B> OptionOf<A>.traverse(GA: Applicative<G>, f: (A) -> Kind<G, B>): Kind<G, Option<B>> = GA.run {
  fix().fold({ just(None) }, { f(it).map { Some(it) } })
}

fun <A, G> OptionOf<Kind<G, A>>.sequence(GA: Applicative<G>): Kind<G, Option<A>> =
  optionTraverse(GA, ::identity)

fun <A, G, B> OptionOf<A>.traverseFilter(GA: Applicative<G>, f: (A) -> Kind<G, Option<B>>): Kind<G, Option<B>> = GA.run {
  fix().fold({ just(None) }, f)
}

@extension
interface OptionTraverse : Traverse<ForOption> {
  override fun <A, B> OptionOf<A>.map(f: (A) -> B): Option<B> =
    fix().map(f)

  override fun <G, A, B> OptionOf<A>.traverse(AP: Applicative<G>, f: (A) -> Kind<G, B>): Kind<G, Option<B>> =
    optionTraverse(AP, f)

  override fun <A> OptionOf<A>.exists(p: (A) -> Boolean): Boolean =
    fix().exists(p)

  override fun <A, B> OptionOf<A>.foldLeft(b: B, f: (B, A) -> B): B =
    fix().foldLeft(b, f)

  override fun <A, B> OptionOf<A>.foldRight(lb: Eval<B>, f: (A, Eval<B>) -> Eval<B>): Eval<B> =
    fix().foldRight(lb, f)

  override fun <A> OptionOf<A>.forAll(p: (A) -> Boolean): Boolean =
    fix().forall(p)

  override fun <A> OptionOf<A>.isEmpty(): Boolean =
    fix().isEmpty()

  override fun <A> OptionOf<A>.nonEmpty(): Boolean =
    fix().nonEmpty()
}

@extension
interface OptionHash<A> : Hash<Option<A>>, OptionEq<A> {

  fun HA(): Hash<A>

  override fun EQ(): Eq<A> = HA()

  override fun Option<A>.hash(): Int = fold({
    None.hashCode()
  }, {
    HA().run { it.hash() }
  })
}

fun <A> Option.Companion.fx(c: suspend MonadSyntax<ForOption>.() -> A): Option<A> =
  Option.monad().fx.monad(c).fix()
