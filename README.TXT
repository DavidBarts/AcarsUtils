This is a set of miscellaneous classes useful for dealing with ACARS
messages.

Probably the most significant thing here is the
info.koosah.acarsutils.wxdecoder package; it can actually interpret and
decode weather observations coded by 10 (at current count) different
airlines. These observations use proprietary, undocumented formats; the
airlines consider them trade secret information. (The supported airlines
are merely those that fly in and out of SeaTac regularly and thus which
I have acquired enough samples from to reverse engineer.)

I have never worked for, or written code for, any airline or aircraft
manufacturer. Therefore all reverse-engineering I have done above is
"clean."

The info.koosah.acarsutils.AcarsMessage class might also be of some
interest, as it provides more message breakdown and explanatory
information than most such open-source ACARS code out there.
