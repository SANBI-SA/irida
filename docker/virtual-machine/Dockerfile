# Galaxy - IRIDA Galaxy Image

FROM phacnml/galaxy-irida-17.01:base

Label maintainer Aaron Petkau, aaron.petkau@phac-aspc.gc.ca

ENV GALAXY_CONFIG_BRAND IRIDA (0.17.0) Galaxy (17.01)

WORKDIR /galaxy-central

RUN install-repository "-r 4287dd541327 --url https://irida.corefacility.ca/galaxy-shed/ -o irida --name flash" && \
	install-repository "-r f9fc830fa47c --url https://irida.corefacility.ca/galaxy-shed/ -o irida --name filter_spades_repeats" && \
	install-repository "-r 51b76a5d78a5 --url https://irida.corefacility.ca/galaxy-shed/ -o irida --name assemblystats" && \
	install-repository "-r 35cb17bd8bf9 --url https://toolshed.g2.bx.psu.edu -o nml --name spades" && \
	install-repository "-r f5e44aad6498 --url https://toolshed.g2.bx.psu.edu -o crs4 --name prokka" && \
	install-repository "-r 9ea374bb0350 --url https://toolshed.g2.bx.psu.edu -o jjohnson --name regex_find_replace" && \
	install-repository "-r 7bc329e1ada4 --url https://irida.corefacility.ca/galaxy-shed/ -o irida --name bundle_collections" && \
	install-repository "-r c970d74729e9 --url https://irida.corefacility.ca/galaxy-shed/ -o irida --name combine_assembly_stats" && \
	install-repository "-r 5c8ff92e38a9 --url https://toolshed.g2.bx.psu.edu -o nml --name sistr_cmd" && \
	find /galaxy-central/tool_deps/ -iname '.git' | xargs -I {} rm -rf {}

RUN apt update && apt install --yes gnuplot && apt-get autoremove -y && apt-get clean && rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*

# Overwrite the tbl2asn that comes with prokka because it's expired!
RUN curl ftp://ftp.ncbi.nih.gov/toolbox/ncbi_tools/converters/by_program/tbl2asn/linux64.tbl2asn.gz > tbl2asn.gz && \
	gunzip --force tbl2asn.gz && \
	chmod +x tbl2asn && \
	find /export/tool_deps/ -executable -name tbl2asn -type f -exec cp tbl2asn '{}' \;

# Install sistr_cmd in conda
RUN /tool_deps/_conda/bin/conda config --add channels conda-forge && \
    /tool_deps/_conda/bin/conda config --add channels defaults && \
    /tool_deps/_conda/bin/conda config --add channels r && \
    /tool_deps/_conda/bin/conda config --add channels bioconda && \
    /tool_deps/_conda/bin/conda create -y --name __sistr_cmd@1.0.2 sistr_cmd=1.0.2 && \
    /tool_deps/_conda/bin/conda create -y --name __samtools@_uv_ samtools && \
    /tool_deps/_conda/bin/conda clean -y -itps

# Expose port 80 (webserver), 21 (FTP server), 8800 (Proxy)
EXPOSE :80
EXPOSE :21
EXPOSE :8800

# Autostart script that is invoked during container start
CMD ["/usr/bin/startup"]
