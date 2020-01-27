import {Injectable} from "@angular/core";
import {HttpClient} from "@angular/common/http";
import {BehaviorSubject, Observable} from "rxjs";
import {TvLibrary} from "../shared/dto/library/TvLibrary";

@Injectable({
  providedIn: 'root'
})
export class LibraryService {

  private backendUrl = "api/library";

  private _tvLibrary = new BehaviorSubject<TvLibrary>(undefined);
  private readonly intervalId: any;
  private updateIntervalInMs = 1000;

  constructor(private httpClient: HttpClient) {
    this.intervalId = setInterval(() => this.updateTvLibrary(), this.updateIntervalInMs)
  }

  public getTvLibrary(): Observable<TvLibrary> {
    let url = `${this.backendUrl}/tv`;
    return this.httpClient.get<TvLibrary>(url);
  }

  ngOnDestroy(): void {
    clearInterval(this.intervalId);
  }

  public getTvLibraryAsObservable(): Observable<TvLibrary>  {
    return this._tvLibrary.asObservable();
  }

  private updateTvLibrary(): void {
    this.getTvLibrary().subscribe(tvLibrary => {
      this._tvLibrary.next(tvLibrary);
    });
  }
}
