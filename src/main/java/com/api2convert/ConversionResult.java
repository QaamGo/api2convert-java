package com.api2convert;

import com.api2convert.exception.Api2ConvertException;
import com.api2convert.http.Transport;
import com.api2convert.model.Job;
import com.api2convert.model.OutputFile;
import java.util.List;

/**
 * The result of a completed conversion.
 *
 * <p>The common case is one output: {@code result.save("out.pdf")}. Jobs that produce several files
 * expose them via {@link #outputs()} and {@link #download()}.
 */
public final class ConversionResult {

    private final Job job;
    private final Transport transport;
    private final int index;
    private final String downloadPassword;

    /**
     * @param downloadPassword password set when the conversion was created; sent automatically when
     *                         downloading a protected output
     */
    public ConversionResult(Job job, Transport transport, int index, String downloadPassword) {
        this.job = job;
        this.transport = transport;
        this.index = index;
        this.downloadPassword = downloadPassword;
    }

    /** The completed job. */
    public Job job() {
        return job;
    }

    /** The selected output file (the first one by default). */
    public OutputFile output() {
        List<OutputFile> outputs = job.output();
        if (index < 0 || index >= outputs.size()) {
            throw new Api2ConvertException("The job produced no output files.");
        }
        return outputs.get(index);
    }

    /** All output files produced by the job. */
    public List<OutputFile> outputs() {
        return job.output();
    }

    /** The download URL of the selected output (self-contained, no auth). */
    public String url() {
        return output().uri();
    }

    public String save(String pathOrDir) {
        return download().save(pathOrDir);
    }

    /**
     * Download the selected output to disk. A download password set at conversion time is applied
     * automatically; pass one here only to override it.
     *
     * @return the path the file was written to
     */
    public String save(String pathOrDir, String downloadPassword) {
        return download().save(pathOrDir, downloadPassword);
    }

    public byte[] contents() {
        return download().contents();
    }

    /**
     * Download the selected output and return its contents (loads into memory). A download password
     * set at conversion time is applied automatically; pass one here only to override it.
     */
    public byte[] contents(String downloadPassword) {
        return download().contents(downloadPassword);
    }

    public FileDownload download() {
        return download(null);
    }

    /**
     * A {@link FileDownload} for a specific output (defaults to the selected one). It carries the
     * download password set at conversion time, so downloads need no password re-supplied.
     */
    public FileDownload download(OutputFile output) {
        return new FileDownload(transport, output != null ? output : output(), downloadPassword);
    }
}
