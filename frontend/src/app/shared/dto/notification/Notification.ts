export interface Notification {
  content: string;
  type: NotificationType;
  /** Optional call-to-action shown on the snackbar (e.g. "Go to Downloads"). */
  action?: NotificationAction;
}

export interface NotificationAction {
  label: string;
  onClick: () => void;
}

export enum NotificationType {
  INFO = 'INFO',
  ERROR = 'ERROR',
  WARNING = 'WARNING',
}
