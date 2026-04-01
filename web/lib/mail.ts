import nodemailer from 'nodemailer'
import type SMTPTransport from 'nodemailer/lib/smtp-transport'

const transportConfig: SMTPTransport.Options = {
  host: process.env.MAIL_HOST,
  port: Number(process.env.MAIL_PORT ?? 587),
  secure: false,
  auth: {
    user: process.env.MAIL_USER,
    pass: process.env.MAIL_PASS,
  },
}

const transporter = nodemailer.createTransport(transportConfig)

export const sendMail = async ({
  to,
  cc,
  bcc,
  subject,
  html,
  from,
}: {
  to: string
  cc?: string
  bcc?: string
  subject: string
  html: string
  from?: string
}) => {
  const options = { to, cc, bcc, subject, html, from }
  if (!from) {
    options.from = process.env.MAIL_FROM
  }

  const info = await transporter.sendMail(options)
  console.log('Message sent: %s', info.messageId)
}
