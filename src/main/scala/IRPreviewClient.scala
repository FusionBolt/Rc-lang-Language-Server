import javax.annotation.Nullable
import java.util

case class IRPreviewPanelUpdateParams(documentUri: String)

case class IRPreviewPanelUpdateResult(@Nullable irs: util.List[util.List[String]])