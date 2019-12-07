package models.pages

import java.time.LocalDate

import cats.implicits._
import com.fijimf.deepfij.schedule.model.{ConferenceStandings, Game, Result, ScheduleRoot, Season, Team, WonLossRecord}
import play.api.Logging

case class TeamPage
(
  t:Team,
  s:Season,
  d:LocalDate,
  games: List[GameLine],
  conferenceStandings:ConferenceStandings,
  overallRecord:WonLossRecord,
  conferenceRecord:WonLossRecord
)

object TeamPage extends Logging {
  def create(root: ScheduleRoot, teamKey: String): Option[TeamPage] = {
    for {
      t <- root.teamByKey.get(teamKey)
      s <- root.seasons.sortBy(-1 * _.year).headOption
      d = LocalDate.now()
      gs = root.gamesForTeamWithResults(t, s)
      conf <- root.conferenceMapping.get(s.id).flatMap(_.get(t.id))
    } yield {
      logger.info(s"teamKey=>$teamKey: ${t.id}, ${s.id}, ${gs.size}, ${conf.id}")
      val standings: ConferenceStandings = root.conferenceStandings(conf, s) //TODO move this function
      val conferenceGames: List[(Game, Option[Result])] = gs.filter(tup => root.isConferenceGame(tup._1))
      TeamPage(t, s, d, gs.map(tup => GameLine.create(root.teamById, t, tup._1, tup._2)), standings, WonLossRecord.from(t, gs), WonLossRecord.from(t, conferenceGames))
    }
  }
}

case class GameLine(date: LocalDate, isAt: Boolean, oppName: String, oppKey: String, isWin: Option[Boolean], score: Option[Int], oppScore: Option[Int])

object GameLine {
  def create(teamMap: Map[Long, Team], t: Team, g: Game, o: Option[Result]): GameLine = {
    if (t.id === g.awayTeamId) {
      GameLine(
        g.date,
        true,
        teamMap.get(g.homeTeamId).map(_.name).getOrElse(""),
        teamMap.get(g.homeTeamId).map(_.key).getOrElse(""),
        o.map(r => r.awayScore > r.homeScore),
        o.map(_.awayScore),
        o.map(_.homeScore)
      )
    } else {
      GameLine(
        g.date,
        false,
        teamMap.get(g.awayTeamId).map(_.name).getOrElse(""),
        teamMap.get(g.awayTeamId).map(_.key).getOrElse(""),
        o.map(r => r.homeScore > r.awayScore),
        o.map(_.homeScore),
        o.map(_.awayScore)
      )
    }
  }
}