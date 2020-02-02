export interface Notification {
  content: string;
  type: NotificationType;
}

export enum NotificationType {
  INFO = "INFO", ERROR = "ERROR"
}
