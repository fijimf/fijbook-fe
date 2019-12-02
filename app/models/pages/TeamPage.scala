package models.pages

import java.time.LocalDate

import com.fijimf.deepfij.schedule.model.{ConferenceStandings, Game, Result, ScheduleRoot, Season, Team, WonLossRecord}
import play.api.Logging

case class TeamPage
(
  t:Team,
  s:Season,
  d:LocalDate,
  games:List[(Game,Option[Result])],
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
      TeamPage(t, s, d, gs, standings, WonLossRecord.from(t, gs), WonLossRecord.from(t, conferenceGames))
    }
  }
}