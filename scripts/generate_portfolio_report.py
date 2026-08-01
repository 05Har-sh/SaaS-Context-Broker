from datetime import datetime
from pathlib import Path

from reportlab.lib import colors
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.lib.units import cm
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.platypus import (
    PageBreak,
    Paragraph,
    SimpleDocTemplate,
    Spacer,
    Table,
    TableStyle,
    Flowable,
)


PROJECT_ROOT = Path(__file__).resolve().parents[1]
OUTPUT_FILE = PROJECT_ROOT / "SaaS_Context_Broker_Portfolio_Report.pdf"


def register_fonts() -> None:
    # Best effort font registration for cleaner PDF rendering on Windows.
    candidate = Path("C:\\Windows\\Fonts\\segoeui.ttf")
    if candidate.exists():
        pdfmetrics.registerFont(TTFont("SegoeUI", str(candidate)))


class ArchitectureDiagram(Flowable):
    def __init__(self):
        super().__init__()
        self.width = 17.5 * cm
        self.height = 9.0 * cm

    def draw_box(self, c, x, y, w, h, title, fill):
        c.setFillColor(fill)
        c.setStrokeColor(colors.HexColor("#1e3a8a"))
        c.roundRect(x, y, w, h, 6, fill=1, stroke=1)
        c.setFillColor(colors.white)
        c.setFont("Helvetica-Bold", 8)
        c.drawCentredString(x + w / 2, y + h / 2 - 2, title)

    def arrow(self, c, x1, y1, x2, y2):
        c.setStrokeColor(colors.HexColor("#334155"))
        c.setLineWidth(1.2)
        c.line(x1, y1, x2, y2)
        c.circle(x2, y2, 1.6, fill=1)

    def draw(self):
        c = self.canv
        x0, y0 = 0, 0

        self.draw_box(c, x0 + 5, y0 + 230, 95, 32, "Slack Webhook", colors.HexColor("#1d4ed8"))
        self.draw_box(c, x0 + 235, y0 + 230, 95, 32, "Jira Webhook", colors.HexColor("#1d4ed8"))
        self.draw_box(c, x0 + 120, y0 + 170, 95, 32, "Incoming Controllers", colors.HexColor("#2563eb"))
        self.draw_box(c, x0 + 120, y0 + 115, 95, 32, "IncidentService", colors.HexColor("#0f766e"))
        self.draw_box(c, x0 + 15, y0 + 55, 95, 32, "TimelineService", colors.HexColor("#0891b2"))
        self.draw_box(c, x0 + 120, y0 + 55, 95, 32, "IncidentRepository", colors.HexColor("#0ea5e9"))
        self.draw_box(c, x0 + 225, y0 + 55, 95, 32, "SlackNotifier", colors.HexColor("#7c3aed"))
        self.draw_box(c, x0 + 120, y0 + 5, 95, 32, "PostgreSQL", colors.HexColor("#475569"))

        self.arrow(c, x0 + 53, y0 + 230, x0 + 155, y0 + 202)
        self.arrow(c, x0 + 283, y0 + 230, x0 + 180, y0 + 202)
        self.arrow(c, x0 + 167, y0 + 170, x0 + 167, y0 + 147)
        self.arrow(c, x0 + 150, y0 + 115, x0 + 70, y0 + 87)
        self.arrow(c, x0 + 167, y0 + 115, x0 + 167, y0 + 87)
        self.arrow(c, x0 + 185, y0 + 115, x0 + 272, y0 + 87)
        self.arrow(c, x0 + 70, y0 + 55, x0 + 145, y0 + 37)
        self.arrow(c, x0 + 167, y0 + 55, x0 + 167, y0 + 37)
        self.arrow(c, x0 + 272, y0 + 55, x0 + 190, y0 + 37)


def build_styles():
    base_styles = getSampleStyleSheet()
    font_name = "SegoeUI" if "SegoeUI" in pdfmetrics.getRegisteredFontNames() else "Helvetica"
    styles = {
        "cover_title": ParagraphStyle(
            "cover_title",
            parent=base_styles["Title"],
            fontName=font_name,
            fontSize=28,
            textColor=colors.HexColor("#0f172a"),
            leading=34,
            spaceAfter=14,
        ),
        "cover_subtitle": ParagraphStyle(
            "cover_subtitle",
            parent=base_styles["Normal"],
            fontName=font_name,
            fontSize=12,
            textColor=colors.HexColor("#334155"),
            leading=18,
        ),
        "h1": ParagraphStyle(
            "h1",
            parent=base_styles["Heading1"],
            fontName=font_name,
            fontSize=18,
            textColor=colors.HexColor("#1e3a8a"),
            leading=22,
            spaceAfter=10,
        ),
        "h2": ParagraphStyle(
            "h2",
            parent=base_styles["Heading2"],
            fontName=font_name,
            fontSize=13,
            textColor=colors.HexColor("#0f766e"),
            leading=16,
            spaceAfter=6,
        ),
        "body": ParagraphStyle(
            "body",
            parent=base_styles["Normal"],
            fontName=font_name,
            fontSize=10.2,
            textColor=colors.HexColor("#111827"),
            leading=15,
        ),
        "small": ParagraphStyle(
            "small",
            parent=base_styles["Normal"],
            fontName=font_name,
            fontSize=9,
            textColor=colors.HexColor("#475569"),
            leading=13,
        ),
    }
    return styles


def section_header(text, styles):
    return [Paragraph(text, styles["h1"]), Spacer(1, 6)]


def p(text, styles):
    return Paragraph(text, styles["body"])


def build_document():
    register_fonts()
    styles = build_styles()

    doc = SimpleDocTemplate(
        str(OUTPUT_FILE),
        pagesize=A4,
        rightMargin=1.7 * cm,
        leftMargin=1.7 * cm,
        topMargin=1.6 * cm,
        bottomMargin=1.6 * cm,
        title="SaaS Context Broker - Portfolio Report",
        author="Project Documentation Generator",
    )

    generated_on = datetime.now().strftime("%d %b %Y, %H:%M")
    story = []

    story.extend(
        [
            Spacer(1, 70),
            Paragraph("SaaS Context Broker", styles["cover_title"]),
            Paragraph("Professional Portfolio Documentation Report", styles["cover_subtitle"]),
            Spacer(1, 18),
            Paragraph(
                "A backend incident orchestration and intelligence engine integrating Slack, Jira, scoring analytics, timeline tracking, and automated escalation workflows.",
                styles["body"],
            ),
            Spacer(1, 24),
            Paragraph(f"Generated on: {generated_on}", styles["small"]),
            Paragraph("Technology Stack: Java 21, Spring Boot 4, Spring Data JPA, PostgreSQL", styles["small"]),
            PageBreak(),
        ]
    )

    story.extend(section_header("1. Executive Summary", styles))
    story.append(
        p(
            "The SaaS Context Broker is an event-driven incident management backend. It ingests updates from Slack and Jira, centralizes incident state, computes severity/risk, provides analytics APIs for dashboards, and automatically escalates critical unresolved incidents through Slack notifications.",
            styles,
        )
    )
    story.append(Spacer(1, 8))
    story.append(
        p(
            "The current implementation includes domain persistence, filtering/search APIs, assignment workflows, timeline observability, system health indicators, configurable scoring weights, scheduled auto-monitoring, and structured validation/error handling suitable for production-like development environments.",
            styles,
        )
    )

    story.extend(section_header("2. Architecture Overview", styles))
    story.append(p("High-level logical flow:", styles))
    story.append(Spacer(1, 8))
    story.append(ArchitectureDiagram())
    story.append(Spacer(1, 10))
    story.append(
        Paragraph(
            "Flow narrative: External webhook events enter controller endpoints -> IncidentService orchestrates lifecycle and scoring -> TimelineService stores immutable event history -> repositories persist state in PostgreSQL -> SlackNotifier sends high-severity alerts -> IncidentMonitor re-evaluates stale incidents on schedule.",
            styles["small"],
        )
    )

    story.extend(section_header("3. Implemented Features and Working", styles))
    features = [
        ["Feature Area", "Current Implementation"],
        ["Inbound event ingestion", "POST /incoming/slack and POST /incoming/jira with request validation and normalized update responses."],
        ["Incident lifecycle management", "CREATE/IN_PROGRESS/RESOLVED/CLOSED lifecycle driven by Jira transitions and message updates."],
        ["Severity engine", "Weighted rule model (urgent keyword, Jira status, stale duration) outputs LOW/MEDIUM/HIGH/CRITICAL."],
        ["Slack escalation", "Automatic Slack alerts for HIGH/CRITICAL with cooldown controls and webhook configurability."],
        ["Timeline & auditability", "Timeline events logged for creation, updates, severity changes, alerts, assignments, and Jira transitions."],
        ["Analytics APIs", "Metrics, health, trend, severity distribution, risk score, highest risk, priority endpoints implemented."],
        ["Assignment workflow", "Incident ownership via POST /incident/{incidentKey}/assign with reassignment tracking."],
        ["Filtering & pagination", "Paged incident list with filters: severity, assignee, stale state, Jira status."],
        ["Error handling", "Global handler for 404/400/500 with structured JSON error payloads."],
        ["Background monitoring", "Scheduled monitor scans incidents, detects stale critical cases, and escalates with cooldown."],
    ]
    feature_table = Table(features, colWidths=[4.5 * cm, 11.8 * cm])
    feature_table.setStyle(
        TableStyle(
            [
                ("BACKGROUND", (0, 0), (-1, 0), colors.HexColor("#1e3a8a")),
                ("TEXTCOLOR", (0, 0), (-1, 0), colors.white),
                ("FONTNAME", (0, 0), (-1, -1), "Helvetica"),
                ("FONTSIZE", (0, 0), (-1, -1), 9),
                ("GRID", (0, 0), (-1, -1), 0.5, colors.HexColor("#cbd5e1")),
                ("BACKGROUND", (0, 1), (-1, -1), colors.HexColor("#f8fafc")),
                ("ROWBACKGROUNDS", (0, 1), (-1, -1), [colors.HexColor("#f8fafc"), colors.HexColor("#eef2ff")]),
                ("VALIGN", (0, 0), (-1, -1), "TOP"),
                ("LEFTPADDING", (0, 0), (-1, -1), 6),
                ("RIGHTPADDING", (0, 0), (-1, -1), 6),
            ]
        )
    )
    story.append(feature_table)
    story.append(PageBreak())

    story.extend(section_header("4. API Capability Surface", styles))
    endpoint_rows = [
        ["Endpoint", "Purpose"],
        ["POST /incoming/slack", "Ingest Slack incident text update, evaluate severity, return alert result."],
        ["POST /incoming/jira", "Ingest Jira status update, sync lifecycle state, re-evaluate severity."],
        ["POST /config/slack", "Store Slack incoming webhook URL with validation."],
        ["GET /incident/{incidentKey}", "Fetch single incident summary response."],
        ["GET /incident/{incidentKey}/timeline", "Fetch merged new + legacy timeline events."],
        ["GET /incident/incident-details/{incidentKey}", "Detailed incident view including assignment and risk."],
        ["GET /incident/all", "Paged incident list with filters (severity/assignee/stale/jiraStatus)."],
        ["GET /incident/metrics", "Dashboard counts: total, active, critical, stale, resolved."],
        ["GET /incident/system-health", "Global health classification and reasons."],
        ["GET /incident/risk-score/{incidentKey}", "Per-incident computed risk score and reason."],
        ["GET /incident/priority/{incidentKey}", "Operational priority label (P1/HIGH/MEDIUM/LOW/RESOLVED)."],
        ["GET /incident/highest-risk", "Incident key with highest current computed risk."],
        ["GET /incident/severity-distribution", "Distribution counts grouped by severity."],
        ["GET /incident/trend", "7-day incident creation trend data."],
        ["POST /incident/{incidentKey}/assign", "Assign/reassign incident ownership and log timeline event."],
    ]
    endpoint_table = Table(endpoint_rows, colWidths=[6.0 * cm, 10.3 * cm])
    endpoint_table.setStyle(
        TableStyle(
            [
                ("BACKGROUND", (0, 0), (-1, 0), colors.HexColor("#0f766e")),
                ("TEXTCOLOR", (0, 0), (-1, 0), colors.white),
                ("GRID", (0, 0), (-1, -1), 0.5, colors.HexColor("#cbd5e1")),
                ("FONTSIZE", (0, 0), (-1, -1), 8.6),
                ("BACKGROUND", (0, 1), (-1, -1), colors.HexColor("#f8fafc")),
                ("ROWBACKGROUNDS", (0, 1), (-1, -1), [colors.HexColor("#f8fafc"), colors.HexColor("#ecfeff")]),
                ("VALIGN", (0, 0), (-1, -1), "TOP"),
            ]
        )
    )
    story.append(endpoint_table)

    story.extend(section_header("5. Core Engine Logic", styles))
    story.append(Paragraph("<b>Severity Scoring Inputs</b>", styles["h2"]))
    story.append(
        p(
            "Scoring weights are externalized in application.properties. Current weights: urgent=50, jiraOpen=30, jiraInProgress=10, stale=20. Staleness threshold is configurable (default: 30 minutes).",
            styles,
        )
    )
    story.append(Spacer(1, 4))
    story.append(
        p(
            "Threshold mapping: score >= 70 => CRITICAL, >= 50 => HIGH, >= 30 => MEDIUM, else LOW. Resolved/closed incidents are terminalized with score 0 for risk/severity calculations in API responses.",
            styles,
        )
    )
    story.append(Spacer(1, 4))
    story.append(
        p(
            "Alerting behavior: HIGH/CRITICAL incidents trigger Slack notifications unless cooldown (10 min) is active. The monitor component has an additional 5-minute escalation cooldown for automated background escalation.",
            styles,
        )
    )

    story.extend(section_header("6. Data Model Summary", styles))
    entity_rows = [
        ["Entity", "Role in System"],
        ["IncidentEntity", "Primary incident aggregate: status, Jira state, severity, timestamps, assignee, alert metadata."],
        ["TimelineEventEntity", "Unified event timeline record for operational/audit observability."],
        ["IncidentEventEntity", "Legacy timeline/event source retained for backward compatibility in timeline API."],
        ["WebhookConfig", "Stores Slack webhook configuration used by notifier service."],
    ]
    entity_table = Table(entity_rows, colWidths=[4.8 * cm, 11.5 * cm])
    entity_table.setStyle(
        TableStyle(
            [
                ("BACKGROUND", (0, 0), (-1, 0), colors.HexColor("#1e293b")),
                ("TEXTCOLOR", (0, 0), (-1, 0), colors.white),
                ("GRID", (0, 0), (-1, -1), 0.5, colors.HexColor("#cbd5e1")),
                ("ROWBACKGROUNDS", (0, 1), (-1, -1), [colors.HexColor("#f8fafc"), colors.HexColor("#f1f5f9")]),
                ("FONTSIZE", (0, 0), (-1, -1), 9),
                ("VALIGN", (0, 0), (-1, -1), "TOP"),
            ]
        )
    )
    story.append(entity_table)

    story.extend(section_header("7. Operational Characteristics", styles))
    story.append(
        p(
            "CORS is enabled for localhost:3000 frontend integration. Validation annotations protect webhook and assignment input quality. A global exception layer standardizes failure payloads across validation, not-found, and internal failures.",
            styles,
        )
    )
    story.append(Spacer(1, 5))
    story.append(
        p(
            "Scheduling is enabled application-wide and IncidentMonitor executes every 10 seconds to assess stale non-terminal incidents and trigger escalation paths. Actuator dependency is included for runtime observability extension.",
            styles,
        )
    )

    story.extend(section_header("8. Current Capability Assessment", styles))
    story.append(
        p(
            "The project currently demonstrates an end-to-end incident intelligence backend with ingestion, state synchronization, dynamic scoring, analytics, and notification orchestration. It is portfolio-ready as a backend systems project showcasing Java/Spring design, domain modeling, API craftsmanship, and operational thinking.",
            styles,
        )
    )
    story.append(Spacer(1, 6))
    story.append(
        p(
            "Primary strengths: clear service separation, configurable scoring, rich API surface, timeline observability, and automated escalation workflows. Current test surface is minimal (context-load test only), leaving room for expanded unit/integration coverage in future iterations.",
            styles,
        )
    )

    doc.build(story)


if __name__ == "__main__":
    build_document()
    print(f"Generated: {OUTPUT_FILE}")
