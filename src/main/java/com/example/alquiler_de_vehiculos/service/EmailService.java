package com.example.alquiler_de_vehiculos.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final String remitente;

    public EmailService(
            @Autowired(required = false) JavaMailSender mailSender,
            @Value("${spring.mail.username:}") String remitente) {
        this.mailSender = mailSender;
        this.remitente = remitente;
    }

    public boolean enviarCodigoVerificacion(String destinatario, String codigo) {
        if (mailSender == null || remitente == null || remitente.isBlank()) {
            System.out.println(">>> [EmailService] JavaMailSender no está configurado con credenciales.");
            return false;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(remitente, "Paul Car's Alquiler de Vehículos");
            helper.setTo(destinatario);
            helper.setSubject("Código de Verificación: " + codigo + " - Paul Car's");

            String contenidoHtml = """
                <div style="font-family: 'Segoe UI', Helvetica, Arial, sans-serif; max-width: 540px; margin: 0 auto; padding: 24px; border: 1px solid #e2e8f0; border-radius: 12px; background: #ffffff;">
                    <div style="text-align: center; margin-bottom: 24px;">
                        <h2 style="color: #0d1b3e; margin: 0; font-size: 24px;">Paul Car's</h2>
                        <span style="color: #64748b; font-size: 13px;">Alquiler de Vehículos en Lima, Perú</span>
                    </div>
                    <div style="background: #f8fafc; border-radius: 8px; padding: 20px; margin-bottom: 20px; border-left: 4px solid #0d1b3e;">
                        <h3 style="margin-top: 0; color: #0d1b3e; font-size: 18px;">Recuperación de Contraseña</h3>
                        <p style="color: #475569; font-size: 14px; line-height: 1.5; margin-bottom: 16px;">
                            Has solicitado restablecer la contraseña de tu cuenta. Utiliza el siguiente código de verificación de 6 dígitos para continuar:
                        </p>
                        <div style="text-align: center; margin: 24px 0;">
                            <span style="display: inline-block; background: #0d1b3e; color: #ffffff; font-size: 32px; font-weight: 800; letter-spacing: 8px; padding: 12px 28px; border-radius: 8px;">
                                %s
                            </span>
                        </div>
                        <p style="color: #64748b; font-size: 12px; text-align: center; margin: 0;">
                            Este código es de un solo uso y vencerá en <strong>15 minutos</strong>.
                        </p>
                    </div>
                    <p style="color: #94a3b8; font-size: 12px; line-height: 1.4; margin: 0;">
                        Si tú no solicitaste este cambio, puedes ignorar este correo de forma segura. Tu contraseña actual no se modificará.
                    </p>
                </div>
            """.formatted(codigo);

            helper.setText(contenidoHtml, true);
            mailSender.send(message);
            
            return true;
        } catch (Exception e) {
            System.err.println(">>> [EmailService] Error al enviar correo a " + destinatario + ": " + e.getMessage());
            return false;
        }
    }

    public boolean enviarRechazoReserva(String destinatario, String nombreCliente, String vehiculo,
                                        String motivoLegible, String observaciones) {
        String obs = (observaciones == null || observaciones.isBlank())
                ? ""
                : "<p style=\"color:#475569;font-size:14px;\"><strong>Observaciones del asesor:</strong> "
                  + escaparHtml(observaciones) + "</p>";
        String html = """
            <div style="font-family:'Segoe UI',Helvetica,Arial,sans-serif;max-width:540px;margin:0 auto;padding:24px;border:1px solid #e2e8f0;border-radius:12px;background:#ffffff;">
                <h2 style="color:#0d1b3e;margin:0 0 4px;">Paul Car's</h2>
                <span style="color:#64748b;font-size:13px;">Alquiler de Vehículos en Lima, Perú</span>
                <div style="background:#fef2f2;border-left:4px solid #dc2626;border-radius:8px;padding:16px;margin:20px 0;">
                    <h3 style="margin:0 0 8px;color:#991b1b;font-size:16px;">Actualización de su solicitud de reserva</h3>
                    <p style="color:#475569;font-size:14px;margin:0 0 8px;">Estimado/a <strong>%s</strong>, se ha evaluado su solicitud para la unidad <strong>%s</strong>.</p>
                    <p style="color:#475569;font-size:14px;margin:0;"><strong>Causal formal:</strong> %s</p>
                </div>
                %s
                <p style="color:#64748b;font-size:13px;line-height:1.5;">Si requiere mayor detalle sobre esta resolución o desea presentar documentación complementaria, le invitamos a contactar a nuestra central de atención o acercarse a cualquiera de nuestras sedes.</p>
            </div>
            """.formatted(escaparHtml(nombreCliente), escaparHtml(vehiculo), escaparHtml(motivoLegible), obs);
        return enviarHtml(destinatario, "Notificación sobre su solicitud de reserva - Paul Car's", html);
    }

    public boolean enviarHtml(String destinatario, String asunto, String html) {
        if (mailSender == null || remitente == null || remitente.isBlank()
                || destinatario == null || destinatario.isBlank()) {
            return false;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(remitente, "Paul Car's Alquiler de Vehículos");
            helper.setTo(destinatario);
            helper.setSubject(asunto);
            helper.setText(html, true);
            mailSender.send(message);
            return true;
        } catch (Exception e) {
            System.err.println(">>> [EmailService] Error al enviar correo a " + destinatario + ": " + e.getMessage());
            return false;
        }
    }

    private String escaparHtml(String texto) {
        if (texto == null) return "";
        return texto.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
