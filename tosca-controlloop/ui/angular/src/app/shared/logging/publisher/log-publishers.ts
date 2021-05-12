import { Observable, of } from 'rxjs';
import { LogEntry } from '../service/log.service';

// ****************************************************
// Log Publisher Abstract Class
// NOTE: This class must be located BEFORE
//       all those that extend this class
// ****************************************************
export abstract class LogPublishers {
  location: string;

  abstract log( record: LogEntry ): Observable<boolean>;

  abstract clear(): Observable<boolean>;

}

// ****************************************************
// Log Console Class
// ****************************************************
export class LogConsole extends LogPublishers {

  constructor() {
    super();
    this.location = 'Console';
  }

  clear(): Observable<boolean> {
    console.clear();
    return of( true );
  }

  log( entry: LogEntry ): Observable<boolean> {
    // Log to console
    console.log( entry.buildToString() );
    return of( true );
  }
}

// ****************************************************
// Log Local Storage Class
// ****************************************************
export class LogLocalStorage extends LogPublishers {

  constructor() {
    super();
    this.location = 'Local Storage';
  }

  // Clear all log entries from local storage
  clear(): Observable<boolean> {
    localStorage.removeItem( this.location );
    return of( true );
  }

  // Append log entry to local storage
  log( entry: LogEntry ): Observable<boolean> {
    let ret = false;
    let values: LogEntry[];

    try {
      // Get previous values from local storage
      values = JSON.parse( localStorage.getItem( this.location ) ) || [];

      // Add new log entry to array
      values.push( entry );

      // Store array into local storage
      localStorage.setItem( this.location, JSON.stringify( values ) );

      // Set return value
      ret = true;
    } catch ( ex ) {
      // Display error in console
      console.log( ex );
    }

    return of( ret );
  }

}
