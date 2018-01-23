package befunge

import cats._, implicits._

import space.{Direction, Up, Down, Left, Right, Point}
import primitives.{Console, Space, Random, Stack}

object language {
  case class Befunge[F[_]](implicit ST: Stack[F, Int],
                           S: Space[F, Char],
                           C: Console[F],
                           R: Random[F, Direction],
                           F: Monad[F]) {
    def number(n: Int): F[Unit] =
      ST.push(n) *> S.advance

    def add: F[Unit] =
      ST.op(_ + _) *> S.advance

    def subtract: F[Unit] =
      ST.op(_ - _) *> S.advance

    def multiply: F[Unit] =
      ST.op(_ * _) *> S.advance

    def divide: F[Unit] =
      ST.op(_ / _) *> S.advance

    def modulo: F[Unit] =
      ST.op(_ % _) *> S.advance

    def not: F[Unit] =
      ST.pop
        .map(v => if (v != 0) 0 else 1)
        .flatMap(ST.push) *> S.advance

    def gt: F[Unit] =
      ST.op((x, y) => if (x > y) 1 else 0) *> S.advance

    def right: F[Unit] =
      S.changeDirection(Right) *> S.advance

    def left: F[Unit] =
      S.changeDirection(Left) *> S.advance

    def up: F[Unit] =
      S.changeDirection(Up) *> S.advance

    def down: F[Unit] =
      S.changeDirection(Down) *> S.advance

    def random: F[Unit] =
      R.oneOf(List(Up, Down, Left, Right))
        .flatMap(S.changeDirection) *> S.advance

    def horizontalIf: F[Unit] =
      ST.pop.flatMap { v =>
        if (v != 0) left else right
      } *> S.advance

    def verticalIf: F[Unit] =
      ST.pop.flatMap { v =>
        if (v != 0) up else down
      } *> S.advance

    // stringMode

    def dup: F[Unit] =
      ST.pop.flatMap(v => ST.push(v) *> ST.push(v)) *> S.advance

    def swap: F[Unit] =
      for {
        b <- ST.pop
        a <- ST.pop
        _ <- ST.push(b)
        _ <- ST.push(a)
        _ <- S.advance
      } yield ()

    def discard: F[Unit] =
      ST.pop.void *> S.advance

    def outputInt: F[Unit] =
      ST.pop
        .map(_.toString + " ")
        .flatMap(C.put) *> S.advance

    def outputAscii: F[Unit] =
      ST.pop
        .map(_.toChar.toString + " ")
        .flatMap(C.put) *> S.advance

    def bridge: F[Unit] =
      S.advance *> S.advance

    def get: F[Unit] =
      for {
        y <- ST.pop
        x <- ST.pop
        v <- S.getAt(Point(x, y))
        _ <- ST.push(v.toInt)
        _ <- S.advance
      } yield ()

    def put: F[Unit] =
      for {
        y <- ST.pop
        x <- ST.pop
        v <- ST.pop
        _ <- S.writeAt(Point(x, y), v.toChar)
        _ <- S.advance
      } yield ()

    def inputInt: F[Unit] =
      C.readInt.flatMap(ST.push) *> S.advance

    def inputChar: F[Unit] =
      C.readChar.flatMap(x => ST.push(x.toInt)) *> S.advance

    def noOp: F[Unit] = F.unit *> S.advance
  }

  object Befunge {
    case class End()

    def fromInstr[F[_]](c: Char)(implicit BF: Befunge[F],
                                 F: MonadError[F, Throwable],
                                 ev2: Stack[F, Int],
                                 ev3: Space[F, Char],
                                 ev4: Console[F]): F[Option[End]] =
      c match {
        case c if Character.isDigit(c) => BF.number(c.toInt).as(None)
        case '+' => BF.add.as(None)
        case '-' => BF.subtract.as(None)
        case '*' => BF.multiply.as(None)
        case '/' => BF.divide.as(None)
        case '%' => BF.modulo.as(None)
        case '!' => BF.not.as(None)
        case '`' => BF.gt.as(None)
        case '>' => BF.right.as(None)
        case '<' => BF.left.as(None)
        case '^' => BF.up.as(None)
        case 'v' => BF.down.as(None)
        case '?' => BF.random.as(None)
        case '_' => BF.horizontalIf.as(None)
        case '|' => BF.verticalIf.as(None)
        // case '"' => toggle string mode
        case ':' => BF.dup.as(None)
        case '\\' => BF.swap.as(None)
        case '$' => BF.discard.as(None)
        case '.' => BF.outputInt.as(None)
        case ',' => BF.outputAscii.as(None)
        case '#' => BF.bridge.as(None)
        case 'g' => BF.get.as(None)
        case 'p' => BF.put.as(None)
        case '&' => BF.inputInt.as(None)
        case '~' => BF.inputChar.as(None)
        case '@' => End().some.pure[F]
        case ' ' => BF.noOp.as(None)
        case c => F.raiseError(new Exception(s"invalid input! $c"))
      }
  }
}
