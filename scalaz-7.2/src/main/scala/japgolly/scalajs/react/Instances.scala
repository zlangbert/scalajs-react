package japgolly.scalajs.react

import scala.annotation.tailrec
import scalaz.{Optional => _, _}
import scalaz.Isomorphism.<~>
import scalaz.effect.IO
import Scalaz.Id

trait ScalazReactInstances {

  implicit val callbackScalazInstance: Monad[CallbackTo] with BindRec[CallbackTo] =
    new Monad[CallbackTo] with BindRec[CallbackTo] {
      override def point[A](a: => A): CallbackTo[A] =
        CallbackTo(a)

      override def bind[A, B](fa: CallbackTo[A])(f: A => CallbackTo[B]): CallbackTo[B] =
        fa >>= f

      override def map[A, B](fa : CallbackTo[A])(f : A => B): CallbackTo[B] =
        fa map f

      override def tailrecM[A, B](f: A => CallbackTo[A \/ B])(a: A): CallbackTo[B] =
        CallbackTo {
          @tailrec
          def go(a: A): B =
            f(a).runNow() match {
              case -\/(n) => go(n)
              case \/-(b) => b
            }
          go(a)
        }
    }

  implicit val callbackOptionScalazInstance: Monad[CallbackOption] with BindRec[CallbackOption] =
    new Monad[CallbackOption] with BindRec[CallbackOption] {
      override def point[A](a: => A): CallbackOption[A] =
        CallbackOption.liftValue(a)

      override def bind[A, B](fa: CallbackOption[A])(f: A => CallbackOption[B]): CallbackOption[B] =
        fa >>= f

      override def map[A, B](fa : CallbackOption[A])(f : A => B): CallbackOption[B] =
        fa map f

      override def tailrecM[A, B](f: A => CallbackOption[A \/ B])(a: A): CallbackOption[B] =
        CallbackOption.liftOption {
          @tailrec
          def go(a: A): Option[B] =
            f(a).get.runNow() match {
              case Some(-\/(n)) => go(n)
              case Some(\/-(b)) => Some(b)
              case None         => None
            }
          go(a)
        }
    }

  implicit val maybeInstance: OptionLike[Maybe] = new OptionLike[Maybe] {
    type O[A] = Maybe[A]
    def map     [A, B](o: O[A])(f: A => B)         : O[B]      = o map f
    def fold    [A, B](o: O[A], b: => B)(f: A => B): B         = o.cata(f, b)
    def foreach [A]   (o: O[A])(f: A => Unit)      : Unit      = o.cata(f, ())
    def isEmpty [A]   (o: O[A])                    : Boolean   = o.isEmpty
    def toOption[A]   (o: O[A])                    : Option[A] = o.toOption
  }

  implicit val callbackToItself: (CallbackTo ~> CallbackTo) =
    new (CallbackTo ~> CallbackTo) { override def apply[A](a: CallbackTo[A]) = a }

  implicit val scalazIoToCallback: (IO ~> CallbackTo) =
    new (IO ~> CallbackTo) { override def apply[A](a: IO[A]) = CallbackTo(a.unsafePerformIO()) }

  val scalazIoToCallbackIso: (CallbackTo <~> IO) =
    new (CallbackTo <~> IO) {
      override val from = scalazIoToCallback
      override val to: CallbackTo ~> IO =
        new (CallbackTo ~> IO) { override def apply[A](a: CallbackTo[A]): IO[A] = IO(a.runNow()) }
    }

  implicit val scalazIdToCallback: (Id ~> CallbackTo) =
    new (Id ~> CallbackTo) { override def apply[A](a: Id[A]) = CallbackTo pure a }

  val scalazIdToCallbackIso: (CallbackTo <~> Id) =
    new (CallbackTo <~> Id) {
      override val from = scalazIdToCallback
      override val to: CallbackTo ~> Id =
        new (CallbackTo ~> Id) { override def apply[A](a: CallbackTo[A]): Id[A] = a.runNow() }
    }
}
