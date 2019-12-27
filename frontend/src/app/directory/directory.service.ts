import {Injectable} from "@angular/core";
import {HttpClient} from "@angular/common/http";
import {BehaviorSubject, Observable} from "rxjs";
import {FileHierarchyDto} from "../shared/dto/directory/FileHierarchyDto";

@Injectable({
  providedIn: 'root'
})
export class DirectoryService {

  private backendUrl = "api/directory";

  private _fileHierarchy = new BehaviorSubject<FileHierarchyDto>(undefined);
  private intervalId: any;
  private updateIntervalInMs = 1000;

  constructor(private httpClient: HttpClient) {
    this.intervalId = setInterval(() => this.updateFileHierarchy(), this.updateIntervalInMs)
  }

  public getFileHierarchy(): Observable<FileHierarchyDto> {
    let url = `${this.backendUrl}/`;
    return this.httpClient.get<FileHierarchyDto>(url);
  }

  ngOnDestroy(): void {
    clearInterval(this.intervalId);
  }

  public getFileHierarchyAsObservable(): Observable<FileHierarchyDto>  {
    return this._fileHierarchy.asObservable();
  }

  private updateFileHierarchy(): void {
    this.getFileHierarchy().subscribe(fileHierarchy => {
      this._fileHierarchy.next(fileHierarchy);
    });
  }
}
