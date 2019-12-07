import {Injectable} from "@angular/core";
import {HttpClient} from "@angular/common/http";
import {Observable} from "rxjs";
import {PirateBayEntry} from "../shared/dto/pirateBay/PirateBayEntry";

@Injectable({
  providedIn: 'root'
})
export class PirateBayService {

  private backendUrl = "api/pirateBay";

  constructor(private httpClient: HttpClient) {
  }

  public searchPirateBay(searchString: string): Observable<PirateBayEntry[]> {
    let url = `${this.backendUrl}?search=${searchString}`;
    return this.httpClient.get<PirateBayEntry[]>(url);
  }
}
