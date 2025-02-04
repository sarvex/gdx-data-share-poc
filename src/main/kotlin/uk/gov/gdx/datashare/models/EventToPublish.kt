package uk.gov.gdx.datashare.models

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.format.annotation.DateTimeFormat
import uk.gov.gdx.datashare.enums.EventType
import uk.gov.gdx.datashare.enums.RegExConstants.SOURCE_ID_REGEX
import java.time.LocalDateTime

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Event Payload for GDX")
data class EventToPublish(
  @Schema(
    description = "Type of event",
    required = true,
    example = "DEATH_NOTIFICATION",
  )
  val eventType: EventType,
  @Schema(
    description = "Date and time when the event took place, default is now",
    required = false,
    type = "date-time",
    example = "2021-12-31T12:34:56",
  )
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  val eventTime: LocalDateTime? = null,
  @Schema(description = "ID that references the event", example = "123456789", maxLength = 80, pattern = SOURCE_ID_REGEX)
  val id: String,
)
