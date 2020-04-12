export interface TorrentEntry {
  name: string;
  magnetLink: string;
  link: string;
  uploadedTime: string;
  size: string;
  uploader: string;
  numberOfSeeders: number;
  numberOfLeechers: number;
  category: string;
  subCategory: string;
  uploaderIsVIP: boolean;
  uploaderIsTrusted: boolean;
}
